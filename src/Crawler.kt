package com.tumn.cat

import com.mongodb.client.MongoCollection
import org.jsoup.Jsoup
import org.bson.Document as BsonDocument
import org.jsoup.nodes.Document
import java.util.Timer
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.timerTask

abstract class Crawler(
		protected val collection: MongoCollection<BsonDocument>,
		protected val userAgent: String = "Tumn-Cat_Bot/v1.0",
		protected val interval: Long
	) {

	private var queue: List<Queue> = emptyList()
	private val timer: Timer = Timer()

	fun start() {
		timer.scheduleAtFixedRate(timerTask {
			if(queue.isNotEmpty()) {
				val f = queue.first()
				queue = queue.drop(1)
				f.f.complete(Jsoup.connect(f.uri).userAgent(userAgent).get())
			}
		}, interval, interval)

		crawl()
	}

	abstract fun crawl()

	fun close(){
		timer.cancel()
	}

	fun connectAndGet(uri: String): CompletableFuture<Document> {
		val future = CompletableFuture<Document>()

		queue += Queue(uri, future)

		return future
	}
}

data class Content(val _id: String?, val uri: String, val content: String)

data class Queue (
		val uri: String,
		val f: CompletableFuture<Document>
)