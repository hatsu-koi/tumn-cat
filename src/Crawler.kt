package com.tumn.cat

import com.beust.klaxon.Klaxon
import com.mongodb.client.MongoCollection
import org.apache.http.client.utils.URIBuilder
import org.apache.http.client.utils.URLEncodedUtils
import org.bson.Document as BsonDocument
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Arrays

abstract class Crawler(
		protected val collection: MongoCollection<BsonDocument>,
		protected val userAgent: String = "Tumn-Cat_Bot/v1.0",
		protected val interval: Long
	) {
	abstract fun crawl()

	fun connectAndGet(uri: String): Document = Jsoup.connect(uri).userAgent(userAgent).get()
}

data class Content(val _id: String?, val uri: String, val content: String)

val NAVER_NEWS_HEADLINES_URI: String = "http://news.naver.com/main/history/mainnews/index.nhn?date=%s&time=00:00"
val NAVER_NEWS_COMMENT_URI: String = "https://apis.naver.com/commentBox/cbox/web_naver_list_jsonp.json"

class NaverNewsCrawler(
		collection: MongoCollection<BsonDocument>,
		userAgent: String,
		interval: Long,
		private val from: Date,
		private val to: Date
	):
		Crawler(collection, userAgent, interval) {
	init {
		if(to.before(from)){
			throw IllegalArgumentException("Invalid range of date given")
		}
	}

	override fun crawl() {
		var visited = 0
		var comments = 0

		val c = Calendar.getInstance()
		c.time = from
		while(c.time.before(to)){
			getListForDate(c.time).forEach {
				val uri = URI(it)
				if(uri.host == "history.news.naver.com") {
					getHistoryNewsList(it).forEach { history ->
						visited++
						getCommentsList(history).let {
							it.forEach {
								collection.insertOne(BsonDocument("url", it.uri).append("content", it.content))
							}
							comments += it.size
						}

						print("\rVisit: %d | Comments: %d".format(visited, comments))
					}
				}else if(uri.host == "news.naver.com"){
					visited++
					getCommentsList(it).let {
						it.forEach {
							collection.insertOne(BsonDocument("url", it.uri).append("content", it.content))
						}
						comments += it.size
					}
				}

				print("\rVisit: %d | Comments: %d".format(visited, comments))
			}

			c.add(Calendar.DATE, 1)
		}
	}

	private fun getListForDate(date: Date): ArrayList<String> {
		val list = arrayListOf<String>()

		val formatter = SimpleDateFormat("yyyy-MM-dd")

		connectAndGet(NAVER_NEWS_HEADLINES_URI.format(formatter.format(date))).run {
			select("div.newsnow_tx_inner a").forEach {
				it.attr("href")?.let { href -> list.add(href) }
			}
		}

		return list
	}

	private fun getHistoryNewsList(uri: String): ArrayList<String> {
		val list = arrayListOf<String>()

		connectAndGet(uri).select("div.hissue_cnt a").forEach {
			it.attr("href")?.let { href ->
				if(URI(href).host == "news.naver.com") {
					list.add(href)
				}
			}
		}

		return list
	}

	private fun getCommentsList(uri: String): ArrayList<Content> {
		val list = arrayListOf<Content>()

		val values = URLEncodedUtils.parse(uri, Charset.forName("UTF-8"))

		val builder = URIBuilder(NAVER_NEWS_COMMENT_URI)
		builder.addParameter("lang", "ko")
		builder.addParameter("page", "1")
		builder.addParameter("pageSize", "20")
		builder.addParameter("pool", "cbox5")
		builder.addParameter("sort", "FAVORITE")
		builder.addParameter("ticket", "news")
		var oid: String? = null
		var aid: String? = null
		values.forEach {
			when(it.name) {
				"oid" -> oid = it.value
				"aid" -> aid = it.value
			}
		}

		if(oid == null || aid == null) return list
		builder.addParameter("objectId", "news%s,%s".format(oid, aid))

		val res = Jsoup.connect(builder.build().toString())
				.userAgent(userAgent)
				.header("Referer", uri)
				.ignoreContentType(true)
				.execute().body()
				.replace("_callback(", "").let {
					it.substring(0, it.length - 2)
				}


		Klaxon().parse<JsonResponse>(res)?.let {
			if(it.success){
				it.result.commentList.forEach {
					list.add(Content(null, uri, it.contents))
				}
			}
		}

		return list
	}

	override fun toString(): String = "Naver News Crawler"
}

data class JsonResponse (
		val success: Boolean,
		val result: Result
)

data class Result (
		val commentList: Array<Comment>
) {
	override fun equals(other: Any?): Boolean {
		return this === other
	}

	override fun hashCode(): Int {
		return Arrays.hashCode(commentList)
	}
}

data class Comment (
		val contents: String
)