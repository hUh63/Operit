package com.ai.assistance.operit.ui.features.settings.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.api.chat.AIServiceFactory
import com.ai.assistance.operit.api.chat.EnhancedAIService
import com.ai.assistance.operit.data.model.FunctionType
import com.ai.assistance.operit.data.model.ModelConfigSummary
import com.ai.assistance.operit.data.preferences.FunctionalConfigManager
import com.ai.assistance.operit.data.preferences.ModelConfigManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunctionalConfigScreen(
        onBackPressed: () -> Unit = {},
        onNavigateToModelConfig: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 配置管理器
    val functionalConfigManager = remember { FunctionalConfigManager(context) }
    val modelConfigManager = remember { ModelConfigManager(context) }

    // 配置映射状态
    val configMapping =
            functionalConfigManager.functionConfigMappingFlow.collectAsState(initial = emptyMap())

    // 配置摘要列表
    var configSummaries by remember { mutableStateOf<List<ModelConfigSummary>>(emptyList()) }

    // UI状态
    var isLoading by remember { mutableStateOf(true) }
    var showSaveSuccess by remember { mutableStateOf(false) }

    // 加载配置摘要
    LaunchedEffect(Unit) {
        isLoading = true
        configSummaries = modelConfigManager.getAllConfigSummaries()
        isLoading = false
    }

    Scaffold() { paddingValues ->
        if (isLoading) {
            Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                    modifier =
                            Modifier.fillMaxSize()
                                    .padding(paddingValues)
                                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                    CardDefaults.cardColors(
                                            containerColor =
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(
                                                            alpha = 0.7f
                                                    )
                                    )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                        text = "功能模型配置设置",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                    text = "为不同功能设置单独的模型配置，使每个功能都能使用最适合的API设置。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Divider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )

                            Row(
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .clickable { onNavigateToModelConfig() }
                                                    .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                    Text(
                                            text = "管理所有模型配置",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "管理模型配置",
                                            tint = MaterialTheme.colorScheme.primary
                                    )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 功能类型列表
                items(FunctionType.values()) { functionType ->
                    val currentConfigId =
                            configMapping.value[functionType]
                                    ?: FunctionalConfigManager.DEFAULT_CONFIG_ID
                    val currentConfig = configSummaries.find { it.id == currentConfigId }

                    FunctionConfigCard(
                            functionType = functionType,
                            currentConfig = currentConfig,
                            availableConfigs = configSummaries,
                            onConfigSelected = { configId ->
                                scope.launch {
                                    functionalConfigManager.setConfigForFunction(
                                            functionType,
                                            configId
                                    )
                                    // 刷新服务实例
                                    EnhancedAIService.refreshServiceForFunction(
                                            context,
                                            functionType
                                    )
                                    showSaveSuccess = true
                                }
                            }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    // 重置按钮
                    OutlinedButton(
                            onClick = {
                                scope.launch {
                                    functionalConfigManager.resetAllFunctionConfigs()
                                    // 刷新所有服务实例
                                    EnhancedAIService.refreshAllServices(context)
                                    showSaveSuccess = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("重置所有功能至默认配置")
                    }

                    // 成功提示
                    androidx.compose.animation.AnimatedVisibility(
                            visible = showSaveSuccess,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors =
                                        CardDefaults.cardColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.primaryContainer
                                        )
                        ) {
                            Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                        text = "配置已保存",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        LaunchedEffect(showSaveSuccess) {
                            kotlinx.coroutines.delay(2000)
                            showSaveSuccess = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FunctionConfigCard(
        functionType: FunctionType,
        currentConfig: ModelConfigSummary?,
        availableConfigs: List<ModelConfigSummary>,
        onConfigSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val modelConfigManager = remember { ModelConfigManager(context) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Result<String>?>(null) }

    LaunchedEffect(testResult) {
        if (testResult != null) {
            kotlinx.coroutines.delay(5000)
            testResult = null
        }
    }

    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border =
                    BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 功能标题和描述
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = getFunctionDisplayName(functionType),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                )

                Text(
                        text = getFunctionDescription(functionType),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 当前配置
                Surface(
                        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                    text = "当前配置: ${currentConfig?.name ?: "默认配置"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                            )

                            if (currentConfig != null) {
                                Text(
                                        text = "模型: ${currentConfig.modelName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // 测试结果显示
                            androidx.compose.animation.AnimatedVisibility(
                                visible = testResult != null,
                                enter = fadeIn() + slideInHorizontally(),
                                exit = fadeOut() + slideOutHorizontally()
                            ) {
                                testResult?.let { result ->
                                    val isSuccess = result.isSuccess
                                    val message =
                                            if (isSuccess) result.getOrNull() ?: "成功"
                                            else "失败: ${result.exceptionOrNull()?.message?.take(30)}"
                                    val color =
                                            if (isSuccess) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.error
                                    val icon =
                                            if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Icon(
                                                icon,
                                                contentDescription = null,
                                                tint = color,
                                                modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                                message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = color,
                                                maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 测试按钮
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        isTestingConnection = true
                                        testResult = null
                                        try {
                                            val configId =
                                                    currentConfig?.id
                                                            ?: FunctionalConfigManager.DEFAULT_CONFIG_ID
                                            val fullConfig =
                                                    modelConfigManager.getModelConfigFlow(configId).first()

                                            val service =
                                                    AIServiceFactory.createService(
                                                            apiProviderType = fullConfig.apiProviderType,
                                                            apiEndpoint = fullConfig.apiEndpoint,
                                                            apiKey = fullConfig.apiKey,
                                                            modelName = fullConfig.modelName
                                                    )
                                            testResult = service.testConnection()
                                        } catch (e: Exception) {
                                            testResult = Result.failure(e)
                                        }
                                        isTestingConnection = false
                                    }
                                },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                if (isTestingConnection) {
                                    CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                            Icons.Default.Dns,
                                            contentDescription = "测试连接",
                                            modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("测试", style = MaterialTheme.typography.bodySmall)
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Icon(
                                    imageVector =
                                            if (expanded) Icons.Default.KeyboardArrowUp
                                            else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "展开",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 配置列表
            Box(modifier = Modifier.fillMaxWidth()) {
                androidx.compose.animation.AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                                text = "选择配置",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                        )

                        availableConfigs.forEach { config ->
                            val isSelected =
                                    config.id ==
                                            (currentConfig?.id
                                                    ?: FunctionalConfigManager.DEFAULT_CONFIG_ID)

                            Surface(
                                    modifier =
                                            Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                                onConfigSelected(config.id)
                                                expanded = false
                                            },
                                    shape = RoundedCornerShape(8.dp),
                                    color =
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surface,
                                    border =
                                            BorderStroke(
                                                    width = if (isSelected) 0.dp else 0.5.dp,
                                                    color =
                                                            if (isSelected)
                                                                    MaterialTheme.colorScheme.primary
                                                            else
                                                                    MaterialTheme.colorScheme
                                                                            .outlineVariant.copy(
                                                                            alpha = 0.5f
                                                                    )
                                            )
                            ) {
                                Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "已选择",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = config.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight =
                                                        if (isSelected) FontWeight.Bold
                                                        else FontWeight.Normal,
                                                color =
                                                        if (isSelected)
                                                                MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onSurface
                                        )

                                        Text(
                                                text = config.modelName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f)
            )
        }
    }
}

// 获取功能类型的显示名称
fun getFunctionDisplayName(functionType: FunctionType): String {
    return when (functionType) {
        FunctionType.CHAT -> "对话功能"
        FunctionType.SUMMARY -> "对话总结"
        FunctionType.PROBLEM_LIBRARY -> "问题库管理"
        FunctionType.FILE_BINDING -> "文件绑定处理"
        FunctionType.UI_CONTROLLER -> "UI 控制器"
    }
}

// 获取功能类型的描述
fun getFunctionDescription(functionType: FunctionType): String {
    return  when (functionType) {
        FunctionType.CHAT -> "主要的对话功能，用于与用户进行日常对话交互"
        FunctionType.SUMMARY -> "生成对话总结，方便追踪重要信息"
        FunctionType.PROBLEM_LIBRARY -> "用于问题库的分析和管理"
        FunctionType.FILE_BINDING ->
                "处理文件内容的智能绑定与混合，提供更精确的代码处理能力。建议使用快速的廉价模型如Gemini Flash，不建议使用DeepSeek R1"
        FunctionType.UI_CONTROLLER -> "用于UI界面的自动遍历与操作，会被主模型调用。建议使用廉价快速模型。"
    }
}
