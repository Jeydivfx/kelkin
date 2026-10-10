from rewrite_support import *
j='app/src/main/java/com/example/kelkin/'
p=j+'SplashActivity.kt'
imports(p,['androidx.core.content.res.ResourcesCompat','kotlinx.coroutines.CancellationException','kotlinx.coroutines.TimeoutCancellationException'])
edit(p,'resources.getFont(R.font.bebas_neue_rgular)','ResourcesCompat.getFont(this@SplashActivity, R.font.bebas_neue_rgular)')
method(p,'navigateToMain','''    private fun navigateToMain() {
        if (isNavigated) return
        isNavigated = true
        exoPlayer?.release(); exoPlayer = null
        lifecycleScope.launch {
            val next = try {
                if (FirebaseAuth.getInstance().currentUser != null && UserManager.activation() == UserManager.Activation.ACTIVE) MainActivity::class.java else LoginActivity::class.java
            } catch (e: TimeoutCancellationException) { LoginActivity::class.java }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { LoginActivity::class.java }
            startActivity(Intent(this@SplashActivity, next)); finish()
        }
    }''')
edit(p,'private const val VIDEO_DURATION = 6000L','private const val VIDEO_DURATION = 2000L')
edit(p,'val waitTime = maxOf(videoDuration, VIDEO_DURATION)','val waitTime = minOf(videoDuration.coerceAtLeast(VIDEO_DURATION), 3000L)')
edit(p,'    override fun onPause()', '''    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || keyCode == android.view.KeyEvent.KEYCODE_ENTER) { navigateToMain(); return true }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause()''')

p=j+'MainActivity.kt'
imports(p,['androidx.navigation.navOptions','androidx.lifecycle.lifecycleScope','com.example.kelkin.utils.UserManager','kotlinx.coroutines.launch','kotlinx.coroutines.CancellationException'])
edit(p,'            navController.navigate(destinationId)','''            navController.navigate(destinationId, null, navOptions {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            })''')
edit(p,'    private val navController:', '''    private val destinationListener = NavController.OnDestinationChangedListener { _, _, _ -> isSidebarExpanded.value = false }
    private val navController:''',1)
text=read(p)
text=re.sub(r'        navController.addOnDestinationChangedListener \{.*?        \}\n\n', '        navController.addOnDestinationChangedListener(destinationListener)\n\n',text, count=1,flags=re.S)
write(p,text)
edit(p,'navController.removeOnDestinationChangedListener { _, _, _ -> }','navController.removeOnDestinationChangedListener(destinationListener)')
edit(p,'            show()\n        }','            show()\n            findViewById<Button>(R.id.btnExitNo).requestFocus()\n        }')
# A foreground active-status listener revokes access without treating a transport error as inactivity.
edit(p,'    override fun onDestroy()', '''    private var activationListener: com.google.firebase.database.ValueEventListener? = null
    private var activationRef: com.google.firebase.database.DatabaseReference? = null
    override fun onStart() {
        super.onStart()
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) { openLogin(); return }
        val ref = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users").child(uid).child("isActive")
        activationRef = ref
        activationListener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (snapshot.getValue(Boolean::class.java) != true) openLogin()
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                android.widget.Toast.makeText(this@MainActivity, "بررسی حساب انجام نشد؛ اتصال را بررسی کنید", android.widget.Toast.LENGTH_LONG).show()
            }
        }.also { ref.addValueEventListener(it) }
    }
    private fun openLogin() {
        startActivity(android.content.Intent(this, LoginActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }
    override fun onStop() {
        activationListener?.let { activationRef?.removeEventListener(it) }
        activationListener = null
        super.onStop()
    }

    override fun onDestroy()''')

p=j+'utils/SidebarComponent.kt'
text=read(p)
text=text.replace('            selectedItem.requestFocus()','            // Preserve the item the user entered; never move focus merely to expand the menu.')
text=text.replace('val effectiveFocused = isFocused && (isStabilized || isDefaultItem)','val effectiveFocused = isFocused')
text=text.replace('R.id.moviesFragment, R.id.movieDetailFragment','R.id.moviesFragment, R.id.movieDetailFragment, R.id.movieListFragment').replace('R.id.seriesFragment, R.id.seriesDetailsFragment','R.id.seriesFragment, R.id.seriesDetailsFragment, R.id.seriesListFragment')
text=text.replace('fontSize = 13.sp','fontSize = 16.sp').replace('contentDescription = null','contentDescription = text').replace('Modifier.size(20.dp)','Modifier.size(24.dp)')
write(p,text)

p='app/build.gradle.kts'
text=read(p).replace('import org.gradle.internal.execution.caching.CachingState.enabled\n\n','')
text=text.replace('storeFile = file("${project.rootDir}/app/kelkin.jks")','storeFile = file(providers.environmentVariable("KELKIN_KEYSTORE_PATH").orElse("${project.rootDir}/app/kelkin.jks").get())')
text=text.replace('storePassword = "Kelkin1234"','storePassword = providers.environmentVariable("KELKIN_KEYSTORE_PASSWORD").orNull').replace('keyAlias = "key0"','keyAlias = providers.environmentVariable("KELKIN_KEY_ALIAS").orElse("key0").get()').replace('keyPassword = "Kelkin1234"','keyPassword = providers.environmentVariable("KELKIN_KEY_PASSWORD").orNull')
text=text.replace('versionCode = 11','versionCode = 12').replace('versionName = "1.79"','versionName = "1.80"')
lines=text.splitlines()
seen=set(); out=[]
for line in lines:
    stripped=line.strip()
    if any(x in line for x in ['com.google.android.exoplayer:', 'org.videolan.android:', 'val composeBom', 'implementation(composeBom)', 'implementation("androidx.activity:activity-compose:', 'implementation("androidx.tv:tv-', 'implementation("androidx.compose.ui:', 'implementation("androidx.compose.material:', 'implementation("org.jsoup:', 'annotationProcessor("com.github.bumptech.glide:']):
        continue
    if stripped.startswith('implementation('):
        if stripped in seen: continue
        seen.add(stripped)
    out.append(line)
text='\n'.join(out)+'\n'
text=text.replace('implementation("com.google.firebase:firebase-config:21.6.3")','implementation("com.google.firebase:firebase-config")').replace('implementation("com.google.firebase:firebase-auth-ktx:22.3.0")','implementation("com.google.firebase:firebase-auth")')
text=text.replace('implementation("androidx.media3:media3-exoplayer-hls:1.2.0")','implementation("androidx.media3:media3-exoplayer-hls:1.2.0")\n    implementation("androidx.media3:media3-exoplayer-dash:1.2.0")\n    implementation("androidx.media3:media3-session:1.2.0")\n    testImplementation("junit:junit:4.13.2")\n    testImplementation("org.robolectric:robolectric:4.14.1")\n    androidTestImplementation("androidx.test:runner:1.6.2")\n    androidTestImplementation("androidx.test.ext:junit:1.2.1")\n    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")\n    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1")')
text=text.replace('        versionName = "1.80"','        versionName = "1.80"\n        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"')
text=text.replace('    buildFeatures {','    testOptions { unitTests.isIncludeAndroidResources = true }\n    buildFeatures {')
write(p,text)
p='app/src/main/res/layout/activity_player.xml'
text=read(p)
text=re.sub(r'    <org.videolan.libvlc.util.VLCVideoLayout.*?/>','',text,flags=re.S)
write(p,text)
write('local.properties','sdk.dir=C:/Users/javad/AppData/Local/Android/Sdk\n')
edit('.gitignore','', '',1) if False else None
write('.gitignore',read('.gitignore')+'\n*.jks\n*.keystore\nkelkin-before-fixes.zip\napp/google-services.json\n')
