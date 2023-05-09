package com.example.bookgroundmusic.DataClass

data class SentimentAnalysisResponse(
    val document: Document,
    val sentences: List<Sentence>
)

data class Document(
    val sentiment: String,
    val confidence: Confidence
)

data class Confidence(
    val neutral: Float,
    val positive: Float,
    val negative: Float
)

data class Sentence(
    val content: String,
    val offset: Int,
    val length: Int,
    val sentiment: String,
    val confidence: Confidence,
    val highlights: List<Highlight>
)

data class Highlight(
    val offset: Int,
    val length: Int
)