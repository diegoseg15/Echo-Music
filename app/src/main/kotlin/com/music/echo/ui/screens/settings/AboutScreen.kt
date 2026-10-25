@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package echo.music.iad1tya.ui.screens.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import echo.music.iad1tya.BuildConfig
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.R
import echo.music.iad1tya.ui.component.IconButton
import echo.music.iad1tya.ui.component.Material3SettingsGroup
import echo.music.iad1tya.ui.component.Material3SettingsItem
import echo.music.iad1tya.ui.utils.backToMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
  navController: NavController,
  scrollBehavior: TopAppBarScrollBehavior,
  onBack: (() -> Unit)? = null,
  highlightKey: String? = null
) {
  val uriHandler = LocalUriHandler.current
  val context = LocalContext.current

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.surface,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = stringResource(R.string.about),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = { onBack?.invoke() ?: navController.navigateUp() },
            onLongClick = navController::backToMain,
          ) {
            Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
          }
        },
        windowInsets = TopAppBarDefaults.windowInsets,
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
          ),
        scrollBehavior = scrollBehavior,
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier =
        Modifier.fillMaxSize()
          .windowInsetsPadding(
            LocalPlayerAwareWindowInsets.current.only(
              WindowInsetsSides.Horizontal,
            ),
          ),
      contentPadding =
        PaddingValues(
          start = 16.dp,
          top = innerPadding.calculateTopPadding() + 8.dp,
          end = 16.dp,
          bottom =
            androidx.compose.foundation.layout.WindowInsets.systemBars
              .asPaddingValues()
              .calculateBottomPadding() + 32.dp,
        ),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item { AboutAppCard() }

      item {
        Material3SettingsGroup(
          title = "Desarrollador",
          items =
            listOf(
              Material3SettingsItem(
                icon = painterResource(R.drawable.website),
                title = { Text("Sitio Web") },
                description = { Text("https://portfolio-77060.web.app/") },
                onClick = { uriHandler.openUri("https://portfolio-77060.web.app/") }
              ),
              Material3SettingsItem(
                icon = painterResource(R.drawable.ic_instagram_new),
                title = { Text("Instagram") },
                description = { Text("@diegoseg_15") },
                onClick = { uriHandler.openUri("https://instagram.com/diegoseg_15") }
              ),
              Material3SettingsItem(
                icon = painterResource(R.drawable.github),
                title = { Text("GitHub") },
                description = { Text("diegoseg15") },
                onClick = { uriHandler.openUri("https://github.com/diegoseg15") }
              )
            )
        )
      }

      item {
        Material3SettingsGroup(
          title = "Repositorio",
          items =
            listOf(
              Material3SettingsItem(
                icon = painterResource(R.drawable.github),
                title = { Text("Código Fuente") },
                description = { Text("diegoseg15/Echo-Music") },
                onClick = { uriHandler.openUri("https://github.com/diegoseg15/Echo-Music") }
              )
            )
        )
      }

      item {
        Material3SettingsGroup(
          title = "Agradecimientos y Base Original",
          items =
            listOf(
              Material3SettingsItem(
                icon = painterResource(R.drawable.github),
                title = { Text("iad1tya") },
                description = { Text("Creador original de Echo Music") },
                onClick = { uriHandler.openUri("https://github.com/iad1tya") }
              )
            )
        )
      }

      item {
        Material3SettingsGroup(
          title = "Comunidad",
          items =
            listOf(
              Material3SettingsItem(
                icon = painterResource(R.drawable.ic_discord_new),
                title = { Text("Discord") },
                description = { Text("discord.gg/Xt5hgsJJuA") },
                onClick = { uriHandler.openUri("https://discord.gg/Xt5hgsJJuA") }
              )
            )
        )
      }

    }
  }
}

@Composable
private fun AboutAppCard() {
  Column(
    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var isEasterEggActive by remember { mutableStateOf(false) }
    val rotation by
      animateFloatAsState(
        targetValue = if (isEasterEggActive) 180f else 0f,
        animationSpec =
          spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "flip"
      )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by
      animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec =
          spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
          ),
        label = "scale"
      )

    Box(
      modifier =
        Modifier.size(100.dp)
          .graphicsLayer {
            rotationY = rotation
            scaleX = scale
            scaleY = scale
            cameraDistance = 12f * density
          }
          .clip(RoundedCornerShape(22.dp))
          .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = { isEasterEggActive = !isEasterEggActive }
          ),
      contentAlignment = Alignment.Center
    ) {
      if (rotation <= 90f) {
        Image(
          painter = painterResource(R.mipmap.vylune_crimson_icon),
          contentDescription = null,
          modifier = Modifier.fillMaxSize()
        )
      } else {
        coil3.compose.AsyncImage(
          model = "https://github.com/diegoseg15.png",
          contentDescription = null,
          modifier =
            Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }, // Un-flip the backside image
          contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
      }
    }

    Spacer(Modifier.height(4.dp))

    Text(
      text = if (rotation <= 90f) "VYLUNE" else "Desarrollado por diegoseg15",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )

    Text(
      text = "Your music. Your universe.",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.primary,
    )

    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
      Text(
        text = "Una marca concebida para una experiencia musical moderna, personal y sin interrupciones.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
      )
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
      ) {
        Text(
          text = BuildConfig.VERSION_NAME,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Medium,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
      }
      if (BuildConfig.DEBUG) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
        ) {
          Text(
            text = "DEBUG",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      } else {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
        ) {
          Text(
            text = BuildConfig.ARCHITECTURE.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }
    }
  }
}
