package com.tumn.cat.crawler

import com.mongodb.client.MongoCollection
import com.tumn.cat.Crawler
import org.bson.Document
import java.util.LinkedList

const val ILBE_BOARD_URI = "http://www.ilbe.com/index.php?mid=%s&page=%d"
const val ILBE_URI = "http://www.ilbe.com/%d"

class IlbeCrawler(
		collection: MongoCollection<Document>,
		userAgent: String,
		interval: Long,
		private val boardId: String,
		private val count: Int
): Crawler(collection, userAgent, interval) {
	override fun crawl() {
		var page = 1
		var num = count

		val list = LinkedList<String>()

		print("* %d left".format(count))
		while(num > 0) {
			connectAndGet(ILBE_BOARD_URI.format(boardId, page)).thenApply {
				it.select("td.title>a").forEach {
					if(num > 0){
						it.attr("href")?.let {
							if(Regex("""^https?://.*""").matches(it)){
								list.add(it)
								num--
							}
						}
					}
				}
			}.join()

			list.iterator().forEach { uri -> // FIXME Handle 404
				connectAndGet(uri).thenApply {
					collection.insertOne(
							Document("content", it.select(".xe_content").text())
									.append("uri", uri))
				}.join()
			}
			list.clear()
			print("\r* %d left".format(num))

			page++
		}

		close()
	}
}