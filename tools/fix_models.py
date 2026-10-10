from rewrite_support import *
path='app/src/main/java/com/example/kelkin/DataClass/Models.kt'
edit(path, 'val seasons: Map<String, Map<String, String>> = emptyMap(),', 'val seasons: Map<String, Map<String, String>> = emptyMap(),\n    var remoteKey: String = "",')
edit(path, 'var quality: String = ""\n) : Serializable {', 'var quality: String = "",\n    var contentType: String = "movie",\n    var remoteKey: String = "",\n    var seasonKey: String = "",\n    var episodeKey: String = ""\n) : Serializable {')
edit('app/src/main/java/com/example/kelkin/object/TranslationHelper.kt', 'پرهزینه صابونی', 'سریال ملودرام')
