from rewrite_support import *
j='app/src/main/java/com/example/kelkin/'
for name in ['AdminDashboardFragment','MovieListFragment','SeriesListFragment','ChannelListFragment']:
    p=j+'Fragments/'+name+'.kt'
    edit(p, ': Fragment(R.layout.', ': AdminFragment(R.layout.')
p=j+'utils/FirebaseManager.kt'
edit(p,'private val database = FirebaseDatabase.getInstance()', 'private val database get() = AdminSession.database')
edit(p,'return com.google.firebase.database.FirebaseDatabase.getInstance()', 'return AdminSession.database')
edit(p,'        val ref = when(refType)', '        if (!AdminSession.isAuthorized) { onResult(false); return }\n        val ref = when(refType)')
p='app/src/main/res/layout/dialog_admin_auth.xml'
edit(p,'android:layout_width="200dp"','android:layout_width="420dp"',1)
edit(p,'android:hint="@string/admin_hint_username"','android:hint="ایمیل مدیر"')
edit(p,'android:inputType="text"','android:inputType="textEmailAddress"')
p=j+'Fragments/AdminDashboardFragment.kt'
edit(p,'        binding.btnManageTV.post {\n            binding.btnManageTV.requestFocus()\n        }','        com.example.kelkin.utils.RecyclerFocusMemory(this, binding.btnManageTV)')

# Secure local Rules artifact. Deployment is documented separately, not silently performed.
rules={"rules": {".read": False,".write":False}}
import json
admin="auth != null && root.child('admins').child(auth.uid).val() === true"
viewer="auth != null && (root.child('users').child(auth.uid).child('isActive').val() === true || root.child('admins').child(auth.uid).val() === true)"
for node in ['movies','series','channels','category','tv_categories','series_categories','language','quality','app_config']:
    rules['rules'][node]={'.read':viewer,'.write':admin}
for node in ['movies','series','channels']:
    rules['rules'][node]['.indexOn']=['id']
rules['rules']['admins']={'$uid':{'.read':"auth != null && auth.uid === $uid",'.write':False,'.validate':'newData.isBoolean()'}}
rules['rules']['users']={'$uid':{'.read':"auth != null && (auth.uid === $uid || root.child('admins').child(auth.uid).val() === true)",'.write':f"({admin}) || (auth != null && auth.uid === $uid && !data.exists() && newData.child('isActive').val() === false)", 'isActive':{'.validate':'newData.isBoolean()'},'email':{'.validate':'newData.isString()'},'createdAt':{'.validate':'newData.isNumber()'},'$other':{'.validate':False}}}
write('firebase/database.rules.json',json.dumps(rules,indent=2)+'\n')
