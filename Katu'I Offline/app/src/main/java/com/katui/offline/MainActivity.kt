package com.katui.offline

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.katui.offline.data.SessionManager
import com.katui.offline.data.repository.UsuarioRepository
import com.katui.offline.ui.components.BottomMenuContent
import com.katui.offline.ui.screens.*
import com.katui.offline.ui.theme.KatuiTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    object Medications : Screen("medications", "Remédios", Icons.Default.MedicalServices)
    object Alarms : Screen("alarms", "Alarmes", Icons.Default.Alarm)
    object Symptoms : Screen("symptoms", "Sintomas", Icons.AutoMirrored.Filled.Assignment)
    object Consultations : Screen("consultations", "Consultas", Icons.Default.Event)
    object Exames : Screen("exames", "Exames", Icons.Default.Description)
    object Receitas : Screen("receitas", "Receitas", Icons.Default.Article)
    object Vaccination : Screen("vaccination", "Vacinas", Icons.Default.Vaccines)
    object FemaleHealth : Screen("female_health", "Saúde Feminina", Icons.Default.Woman)
    object Profile : Screen("profile", "Perfil", Icons.Default.Person)
    object Settings : Screen("settings", "Configurações", Icons.Default.Settings)
    object PrescriptionImport : Screen("prescription_import", "Importar Receita", Icons.Default.Description)
    object Vitals : Screen("vitals", "Sinais Vitais", Icons.Default.MonitorHeart)
    object About : Screen("about", "Sobre o App", Icons.Default.Info)
    object Pregnancy : Screen("pregnancy", "Minha Gestação", Icons.Default.ChildCare)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var usuarioRepository: UsuarioRepository

    private var isReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Tenta sincronizar o modo noturno o mais rápido possível
        lifecycleScope.launch {
            try {
                val darkMode = sessionManager.darkMode.first()
                val mode = when (darkMode) {
                    true -> AppCompatDelegate.MODE_NIGHT_YES
                    false -> AppCompatDelegate.MODE_NIGHT_NO
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                if (AppCompatDelegate.getDefaultNightMode() != mode) {
                    AppCompatDelegate.setDefaultNightMode(mode)
                }
            } catch (e: Exception) {
                // Silently ignore or log
            }
        }

        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !isReady }

        lifecycleScope.launch {
            try {
                val onboarding = sessionManager.onboardingComplete.first()
                if (onboarding) {
                    val existe = usuarioRepository.usuarioPrincipalExiste(1L)
                    if (!existe) {
                        sessionManager.setOnboardingComplete(false)
                        sessionManager.onboardingComplete.filter { !it }.first()
                    }
                }
            } catch (e: Exception) {
                Log.e("KATUI_DEBUG", "Erro na Splash", e)
            } finally {
                isReady = true
            }
        }

        enableEdgeToEdge()
        setContent {
            val darkMode by sessionManager.darkMode.collectAsState(initial = null)
            val fontSize by sessionManager.fontSize.collectAsState(initial = "Normal")
            val dyslexiaFont by sessionManager.dyslexiaFont.collectAsState(initial = false)
            val currentUserId by sessionManager.userId.collectAsState(initial = 1L)
            
            // Observa o usuário atual para obter sua cor de perfil
            val usuarioAtivo by usuarioRepository.getUsuarioPorId(currentUserId).collectAsState(initial = null)
            val seedColor = remember(usuarioAtivo) { 
                com.katui.offline.util.UiTextHelper.getUserColor(usuarioAtivo) 
            }
            
            val useDarkTheme = darkMode ?: isSystemInDarkTheme()
            
            KatuiTheme(
                darkTheme = useDarkTheme, 
                fontSize = fontSize, 
                dyslexiaFont = dyslexiaFont,
                seedColor = seedColor
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainApp(sessionManager, usuarioRepository)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(sessionManager: SessionManager, usuarioRepository: UsuarioRepository) {
    val onboardingComplete by sessionManager.onboardingComplete.collectAsState(initial = null)
    var showHealthProfileOnboarding by remember { mutableStateOf(false) }

    if (onboardingComplete == null) return

    if (onboardingComplete == false) {
        if (showHealthProfileOnboarding) {
            val scope = rememberCoroutineScope()
            OnboardingProfileScreen(onComplete = { scope.launch { sessionManager.setOnboardingComplete(true) } })
        } else {
            PermissionsScreen(onContinue = { showHealthProfileOnboarding = true })
        }
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val scope = rememberCoroutineScope()
    
    val currentUserId by sessionManager.userId.collectAsState(initial = 1L)

    val allScreens = listOf(
        Screen.Dashboard, Screen.Medications, Screen.Symptoms, Screen.Vitals,
        Screen.Consultations, Screen.Exames, Screen.FemaleHealth, Screen.Receitas,
        Screen.Vaccination, Screen.Alarms, Screen.Profile, Screen.Settings,
        Screen.PrescriptionImport, Screen.About, Screen.Pregnancy
    )
    val currentScreen = allScreens.find { it.route == currentRoute }

    // Telas que não mostram o botão de Menu (como onboarding)
    val showNavigation = onboardingComplete == true

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true // Impede que a gaveta "desça" para fora da tela
        )
    )

    // Conexão de scroll aninhado para evitar que a gaveta feche acidentalmente
    // ao scrollar o conteúdo do menu. Isso torna a gaveta "menos sensível".
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Se a gaveta estiver expandida e o usuário arrastar para baixo no conteúdo,
                // consumimos o excesso de scroll para evitar que a gaveta colapse.
                // Isso obriga o usuário a usar a "alça" se quiser realmente fechar a gaveta.
                return if (available.y > 0) available else Offset.Zero
            }
        }
    }

    // Comportamento do botão Voltar do sistema:
    // Sincroniza visibilidade do menu com o status de onboarding
    LaunchedEffect(showNavigation) {
        if (!showNavigation) {
            scaffoldState.bottomSheetState.hide()
        } else {
            // Força o estado PartiallyExpanded no início ou quando showNavigation muda
            scaffoldState.bottomSheetState.partialExpand()
        }
    }

    // Função auxiliar para navegação limpa (usada pelo menu e botões de atalho)
    val navigateCleanly = { route: String ->
        scope.launch { scaffoldState.bottomSheetState.partialExpand() }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Comportamento do botão Voltar do sistema:
    // 1. Se o menu (gaveta) estiver aberto, fecha o menu.
    // 2. Se estiver em qualquer tela que não seja o Dashboard, volta para o Dashboard.
    // 3. Se já estiver no Dashboard, o comportamento padrão (sair do app) acontece.
    BackHandler(enabled = currentRoute != Screen.Dashboard.route || scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
        if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
            scope.launch { scaffoldState.bottomSheetState.partialExpand() }
        } else {
            navigateCleanly(Screen.Dashboard.route)
        }
    }

    // O peekHeight deve ser APENAS o que queremos que apareça acima da barra de sistema.
    // O Scaffold já lida com o WindowInsets se configurado, mas aqui estamos fazendo manual.
    val peekHeight = if (showNavigation) 70.dp else 0.dp

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = peekHeight,
        sheetDragHandle = null,
        sheetSwipeEnabled = true, // Reabilitado para permitir gestos na alça
        // A "sensibilidade" agora é controlada pelo nestedScrollConnection no conteúdo,
        // que impede que o scroll da lista feche a gaveta acidentalmente.
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), // Cantos arredondados no topo
        sheetContent = {
            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.95f)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Barra de Rodapé (Área de Puxar / Toque) — fundo sólido
                    // (primaryContainer, sem transparência) para não deixar o conteúdo
                    // da tela de trás vazar por ela.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                scope.launch {
                                    if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
                                        scaffoldState.bottomSheetState.partialExpand()
                                    } else {
                                        scaffoldState.bottomSheetState.expand()
                                    }
                                }
                            },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            // Indicador de Swipe
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Box(modifier = Modifier.width(40.dp).height(3.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape))
                                Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape))
                                Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape))
                                Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape))
                                Box(modifier = Modifier.width(40.dp).height(3.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape))
                            }
                        }
                    }
                    
                    // Conteúdo do Menu (rola dentro do espaço restante; o padding de baixo
                    // reserva a altura exata da barra de navegação do sistema, então o
                    // conteúdo da gaveta nunca desenha sob os botões do Android).
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .nestedScroll(nestedScrollConnection)
                    ) {
                        BottomMenuContent(
                            onNavigate = { route -> navigateCleanly(route) },
                            onDismiss = { scope.launch { scaffoldState.bottomSheetState.partialExpand() } }
                        )
                    }

                    // FIX: faixa sólida cobrindo exatamente a área da barra de navegação
                    // do sistema, DENTRO do próprio sheetContent. Uma faixa externa com
                    // zIndex alto NÃO funciona aqui: o BottomSheetScaffold desenha o
                    // sheetContent numa camada de composição posterior à do conteúdo
                    // principal, sobrepondo qualquer zIndex externo quando a gaveta expande.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsBottomHeight(WindowInsets.navigationBars)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                }
            }
        },
        topBar = null
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .padding(bottom = innerPadding.calculateBottomPadding())
                .statusBarsPadding()
        ) {
            composable(Screen.Dashboard.route) { 
                DashboardScreen(onNavigate = { route, clearStack -> 
                    if (clearStack) navigateCleanly(route) else navController.navigate(route)
                }) 
            }
            // ... (restante das rotas permanece igual)
            composable(Screen.Medications.route) { 
                MedicationScreen(onNavigateToImport = { navigateCleanly(Screen.PrescriptionImport.route) }) 
            }
            composable(Screen.Alarms.route) { AlarmScreen() }
            composable(Screen.Symptoms.route) { SymptomScreen() }
            composable(Screen.Consultations.route) { ConsultationScreen() }
            composable(Screen.Exames.route) { 
                ExameScreen(onNavigate = { navController.navigate(it) }) 
            }
            composable(Screen.Vaccination.route) { VaccinationScreen() }
            composable(Screen.FemaleHealth.route) { 
                val usuarioAtivo by usuarioRepository.getUsuarioPorId(currentUserId).collectAsState(initial = null)
                val user = usuarioAtivo
                if (user == null) {
                    // Aguarda o carregamento do usuário
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val isSaudeFemininaEnabled = user.sexo == com.katui.offline.data.local.entities.SexoBiologico.FEMININO || user.sexo == com.katui.offline.data.local.entities.SexoBiologico.INTERSEXO
                    
                    if (isSaudeFemininaEnabled) {
                        FemaleHealthScreen(
                            onNavigateToPregnancy = { navController.navigate(Screen.Pregnancy.route) }
                        ) 
                    } else {
                        LaunchedEffect(Unit) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                        }
                    }
                }
            }
            composable(Screen.Pregnancy.route) {
                val usuarioAtivo by usuarioRepository.getUsuarioPorId(currentUserId).collectAsState(initial = null)
                val user = usuarioAtivo
                if (user == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val isSaudeFemininaEnabled = user.sexo == com.katui.offline.data.local.entities.SexoBiologico.FEMININO || user.sexo == com.katui.offline.data.local.entities.SexoBiologico.INTERSEXO
                    
                    if (isSaudeFemininaEnabled) {
                        PregnancyScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToSymptoms = { navigateCleanly(Screen.Symptoms.route) },
                            onNavigateToConsultations = { navigateCleanly(Screen.Consultations.route) }
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                        }
                    }
                }
            }
            composable(Screen.Receitas.route) { 
                PrescriptionScreen(onNavigateToImport = { navigateCleanly(Screen.PrescriptionImport.route) }) 
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToAbout = { navigateCleanly(Screen.About.route) },
                    onClose = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) { SettingsScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.PrescriptionImport.route) {
                PrescriptionImportScreen(
                    onImportComplete = { 
                        navController.navigate(Screen.Medications.route) { 
                            popUpTo(Screen.PrescriptionImport.route) { inclusive = true } 
                        } 
                    },
                    onNavigateToSettings = { navigateCleanly(Screen.Settings.route) }
                )
            }
            composable(Screen.Vitals.route) { VitalsScreen() }
            composable(Screen.About.route) { AboutScreen() }
        }
    }
}
