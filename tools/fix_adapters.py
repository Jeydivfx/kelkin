from rewrite_support import *
j='app/src/main/java/com/example/kelkin/'
for name, model, key in [('MovieGridAdapter','Movie','identity()'),('SeriesGridAdapter','Series','identity()'),('ChannelAdapter','Channel','id.toString()')]:
    p=j+'Adapter/'+name+'.kt'
    imports(p,['com.example.kelkin.utils.*'])
    text=read(p)
    text=text.replace('('+name.replace('Adapter','DiffCallback')+'()) {','('+name.replace('Adapter','DiffCallback')+'()), FocusKeyProvider {') if False else text
    pattern=r'(ListAdapter<[^\n]+>\([^\n]+\)) \{'
    text,n=re.subn(pattern,r'\1, FocusKeyProvider {',text, count=1)
    assert n==1,p
    init='''
    init { setHasStableIds(true) }
    override fun getItemId(position: Int) = stableContentId(getItem(position).KEY)
    override fun focusKey(position: Int) = currentList.getOrNull(position)?.KEY
    override fun positionOfKey(key: String) = currentList.indexOfFirst { it.KEY == key }
'''.replace('KEY',key)
    pos=text.index('    override fun onCreateViewHolder')
    text=text[:pos]+init+'\n'+text[pos:]
    if model!='Channel':
        start=text.index('            val baseTMDB')
        end=text.index('            Glide.with',start)
        text=text[:start]+'            val fullUrl = ImageUrls.poster('+model.lower()+'.posterUrl)\n\n'+text[end:]
        text=text.replace('"اس"','"—"').replace('"--"','"—"')
        text=text.replace('oldItem.tmdb_id == newItem.tmdb_id','oldItem.identity() == newItem.identity()')
        text=text.replace('txtTitle.text = '+model.lower()+'.name_fa','txtTitle.text = '+model.lower()+'.name_fa\n            itemView.contentDescription = '+model.lower()+'.name_fa')
    else:
        text=text.replace('txtName.text = channel.name_fa','txtName.text = channel.name_fa\n            itemView.contentDescription = channel.name_fa')
    write(p,text)

p='app/src/main/res/layout/item_movie_card.xml'
edit(p, 'android:layout_width="100dp"', 'android:layout_width="140dp"')
edit(p, 'android:layout_height="160dp"', 'android:layout_height="210dp"')
edit(p, 'android:layout_height="50dp"', 'android:layout_height="80dp"')
edit(p, '        <ProgressBar', '''        <TextView
            android:id="@+id/movieTitle"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginHorizontal="8dp"
            android:layout_marginBottom="10dp"
            android:maxLines="2"
            android:ellipsize="end"
            android:fontFamily="@font/yekanbakh_bold"
            android:textColor="@color/text_white"
            android:textSize="14sp"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintBottom_toBottomOf="parent" />

        <ProgressBar''')

p=j+'Adapter/CastAdapter.kt'
edit(p,'        val member = cast[position]','        val member = cast[position]\n        holder.itemView.isFocusable = false\n        holder.itemView.isClickable = false')
write('app/src/main/res/layout/item_cast.xml',read('app/src/main/res/layout/item_cast.xml').replace('android:focusable="true"','android:focusable="false"').replace('android:focusableInTouchMode="true"','android:focusableInTouchMode="false"').replace('android:clickable="true"','android:clickable="false"'))

p=j+'Adapter/EpisodesAdapter.kt'
imports(p,['com.example.kelkin.utils.FocusKeyProvider','com.example.kelkin.utils.stableContentId'])
edit(p,') : RecyclerView.Adapter<EpisodesAdapter.EpisodeViewHolder>() {',') : RecyclerView.Adapter<EpisodesAdapter.EpisodeViewHolder>(), FocusKeyProvider {\n    init { setHasStableIds(true) }\n    override fun getItemId(position: Int) = stableContentId(episodes[position].key)\n    override fun focusKey(position: Int) = episodes.getOrNull(position)?.key\n    override fun positionOfKey(key: String) = episodes.indexOfFirst { it.key == key }')
edit(p,'            binding.root.isFocusable = true','            binding.root.isFocusable = true\n            binding.root.contentDescription = "قسمت ${epKey.replace(\"ep\", \"\")}"')
