package com.tumn.cat.crawler

import com.mongodb.client.MongoCollection
import com.tumn.cat.Crawler
import com.tumn.cat.isInteger
import org.bson.Document
import java.util.LinkedList

const val DC_LIST_URI = "http://gall.dcinside.com/board/lists/?id=%s&page=%d"
const val DC_THREAD_URI = "http://gall.dcinside.com/board/view/?id=%s&no=%s"

class DcCrawler(
		collection: MongoCollection<Document>,
		userAgent: String,
		interval: Long,
		private val boardId: String,
		private val count: Int
): Crawler(collection, userAgent, interval) {
	override fun crawl(){
		var page = 1
		var num = count

		val list = LinkedList<String>()

		print("* %d left".format(count))
		while(num > 0) {
			connectAndGet(DC_LIST_URI.format(boardId, page)).thenApply {
				it.select(".t_notice").forEach {
					val id = it.text()
					if (num > 0) {
						if (id.isInteger()) { // '공지' ID를 걸러냄
							list.add(DC_THREAD_URI.format(boardId, id))

							num--
						}
					}
				}
			}.exceptionally { it.printStackTrace() }.join()

			list.iterator().forEach { uri -> // FIXME Handle 404
				connectAndGet(uri).thenApply {
					collection.insertOne(
							Document("content", it.select("div.s_write>table>tbody").text())
							.append("uri", uri))
				}.exceptionally { it.printStackTrace() }.join()
			}
			list.clear()
			print("\r* %d left".format(num))

			page++
		}

		close()
	}
}
