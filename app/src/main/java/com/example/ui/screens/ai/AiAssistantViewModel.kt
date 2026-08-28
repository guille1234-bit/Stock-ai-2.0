package com.example.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiAdvisorService
import com.example.data.ai.StockAiEngine
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.SalesRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.model.AiInsight
import com.example.domain.model.BusinessSettings
import com.example.domain.model.Expense
import com.example.domain.model.Product
import com.example.domain.model.Sale
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MessageSender {
    USER, AI
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AiAssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isThinking: Boolean = false,
    val businessSettings: BusinessSettings = BusinessSettings(),
    val activeInsights: List<AiInsight> = emptyList(),
    val suggestedQuestions: List<String> = listOf(
        "¿Qué producto me deja más ganancia?",
        "¿Qué vendí más en los últimos 7 días?",
        "¿Qué productos debería reponer hoy?",
        "¿Cuánto dinero gané este mes?",
        "¿Qué productos tienen bajo margen?",
        "¿Qué productos están estancados sin rotación?"
    )
)

class AiAssistantViewModel(
    private val productRepository: ProductRepository,
    private val salesRepository: SalesRepository,
    private val expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    private val stockAiEngine: StockAiEngine,
    private val geminiAdvisorService: GeminiAdvisorService
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = MessageSender.AI,
                text = "¡Hola! Soy **Stock AI**, tu asesor inteligente de negocio. Puedo responder preguntas sobre tus productos más rentables, proyección de compras, márgenes y ventas basándome 100% en tus datos reales. ¿En qué te puedo ayudar hoy?"
            )
        )
    )
    private val _isThinking = MutableStateFlow(false)

    // Combine data flows into one data snapshot flow
    private val _businessDataFlow = combine(
        productRepository.allActiveProducts,
        salesRepository.allSales,
        expenseRepository.allExpenses,
        settingsRepository.settingsFlow
    ) { products, sales, expenses, settings ->
        val startOf7d = DateUtils.getStartOfDaysAgo(7)
        val sales7d = sales.filter { it.timestamp >= startOf7d }
        val startOfMonth = DateUtils.getStartOfCurrentMonth()
        val expensesMonth = expenses.filter { it.timestamp >= startOfMonth }

        val insights = stockAiEngine.generateLocalInsights(
            products = products,
            salesLast7Days = sales7d,
            salesLast30Days = sales.filter { it.timestamp >= DateUtils.getStartOfDaysAgo(30) },
            expensesCurrentMonth = expensesMonth,
            currencySymbol = settings.currencySymbol,
            currencyCode = settings.currencyCode
        )
        Pair(settings, insights)
    }

    val uiState: StateFlow<AiAssistantUiState> = combine(
        _messages,
        _isThinking,
        _businessDataFlow
    ) { messages, thinking, (settings, insights) ->
        AiAssistantUiState(
            messages = messages,
            isThinking = thinking,
            businessSettings = settings,
            activeInsights = insights
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AiAssistantUiState()
    )

    fun sendQuestion(questionText: String) {
        if (questionText.isBlank()) return

        val userMsg = ChatMessage(sender = MessageSender.USER, text = questionText.trim())
        _messages.value = _messages.value + userMsg
        _isThinking.value = true

        viewModelScope.launch {
            try {
                val products = productRepository.allActiveProducts.first()
                val sales = salesRepository.allSales.first()
                val expenses = expenseRepository.allExpenses.first()
                val settings = settingsRepository.settingsFlow.first()

                // First attempt local factual engine response
                val localAnswer = stockAiEngine.answerBusinessQuestionLocally(
                    question = questionText,
                    products = products,
                    allSales = sales,
                    allExpenses = expenses,
                    currencySymbol = settings.currencySymbol,
                    currencyCode = settings.currencyCode
                )

                if (localAnswer != null) {
                    _messages.value = _messages.value + ChatMessage(sender = MessageSender.AI, text = localAnswer)
                    _isThinking.value = false
                    return@launch
                }

                // If local patterns don't cover it directly, query Gemini with actual business summary
                val businessSummary = buildString {
                    appendLine("Negocio: ${settings.businessName} (${settings.businessType})")
                    appendLine("Moneda: ${settings.currencySymbol} (${settings.currencyCode})")
                    appendLine("Total productos en catálogo: ${products.size}")
                    appendLine("Productos con bajo stock o agotados: ${products.count { it.isLowStock || it.isOutOfStock }}")
                    val totalRevMonth = sales.filter { it.timestamp >= DateUtils.getStartOfCurrentMonth() }.sumOf { it.totalAmount }
                    val totalCostMonth = sales.filter { it.timestamp >= DateUtils.getStartOfCurrentMonth() }.sumOf { it.totalCost }
                    val totalExpMonth = expenses.filter { it.timestamp >= DateUtils.getStartOfCurrentMonth() }.sumOf { it.amount }
                    val netProfitMonth = totalRevMonth - totalCostMonth - totalExpMonth
                    appendLine("Ventas este mes: $totalRevMonth")
                    appendLine("Gastos este mes: $totalExpMonth")
                    appendLine("Ganancia neta calculada este mes: $netProfitMonth")
                    appendLine("Muestra de productos:")
                    products.take(15).forEach { p ->
                        appendLine("- ${p.name}: Costo ${p.costPrice}, Venta ${p.salePrice}, Stock ${p.currentStock} ${p.unit}, Margen ${p.profitMarginPercent}%")
                    }
                }

                val geminiResult = geminiAdvisorService.queryGeminiWithBusinessContext(
                    userQuestion = questionText,
                    businessContextSummary = businessSummary
                )

                val reply = geminiResult.getOrElse {
                    "He analizado los datos de tu negocio:\n\n" +
                            "• Tienes **${products.size} productos** activos registrados.\n" +
                            "• Hay **${products.count { it.isLowStock }} productos** que necesitan reposición urgente.\n" +
                            "• Tu producto con mayor margen porcentual es **${products.maxByOrNull { it.profitMarginPercent }?.name ?: "N/A"}**.\n\n" +
                            "Si deseas un análisis más detallado, prueba las preguntas rápidas sugeridas o ingresa a la pestaña de Ganancias."
                }

                _messages.value = _messages.value + ChatMessage(sender = MessageSender.AI, text = reply)
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage(
                    sender = MessageSender.AI,
                    text = "Ocurrió un error al procesar tu consulta: ${e.localizedMessage ?: "Error desconocido"}"
                )
            } finally {
                _isThinking.value = false
            }
        }
    }
}
