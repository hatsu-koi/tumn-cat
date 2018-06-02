package com.tumn.cat.crawler

import com.beust.klaxon.Klaxon
import com.mongodb.client.MongoCollection
import com.tumn.cat.Content
import com.tumn.cat.Crawler
import org.apache.http.client.utils.URIBuilder
import org.apache.http.client.utils.URLEncodedUtils
import org.bson.Document
import org.jsoup.Jsoup
import java.net.URI
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Calendar
import java.util.Date

const val NAVER_NEWS_HEADLINES_URI: String = "http://news.naver.com/main/history/mainnews/index.nhn?date=%s&time=00:00"
const val NAVER_NEWS_COMMENT_URI: String = "https://apis.naver.com/commentBox/cbox/web_naver_list_jsonp.json"

class NaverNewsCrawler(
		collection: MongoCollection<Document>,
		userAgent: String,
		interval: Long,
		private val from: Date,
		private val to: Date
):
		Crawler(collection, userAgent, interval) {
	private var comments = 0

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
								collection.insertOne(Document("url", it.uri).append("content", it.content))
							}
							comments += it.size
						}
					}
				}else if(uri.host == "news.naver.com"){
					visited++
					getCommentsList(it).let {
						it.forEach {
							collection.insertOne(Document("url", it.uri).append("content", it.content))
						}
						comments += it.size
					}
				}
			}

			c.add(Calendar.DATE, 1)
		}

		super.close()
	}

	private fun getListForDate(date: Date): ArrayList<String> {
		val list = arrayListOf<String>()

		val formatter = SimpleDateFormat("yyyy-MM-dd")

		connectAndGet(NAVER_NEWS_HEADLINES_URI.format(formatter.format(date))).thenApply {
			it.select("div.newsnow_tx_inner a").forEach {
				it.attr("href")?.let { href -> list.add(href) }
			}
		}.join()

		return list
	}

	private fun getHistoryNewsList(uri: String): ArrayList<String> {
		val list = arrayListOf<String>()

		connectAndGet(uri).thenApply {
			it.select("div.hissue_cnt a").forEach {
				it.attr("href")?.let { href ->
					if(URI(href).host == "news.naver.com") {
						list.add(href)
					}
				}
			}
		}.join()

		return list
	}

	private fun getCommentsList(uri: String): ArrayList<Content> {
		val list = arrayListOf<Content>()

		val values = URLEncodedUtils.parse(uri, Charset.forName("UTF-8"))

		val builder = URIBuilder(NAVER_NEWS_COMMENT_URI)
				.addParameter("lang", "ko")
				.addParameter("page", "1")
				.addParameter("pageSize", "20")
				.addParameter("pool", "cbox5")
				.addParameter("sort", "FAVORITE")
				.addParameter("ticket", "news")
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
					comment() // fu
					list.add(Content(null, uri, it.contents))
				}
			}
		}

		return list
	}

	private fun comment(){
		comments++
		print("\rComments: %d".format(comments))
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