package com.example.ocr

data class SampleReceiptFixture(
    val id: String,
    val title: String,
    val description: String,
    val rawText: String,
    val defaultFilename: String
)

object SampleDocumentFixtures {
    val fixtures = listOf(
        SampleReceiptFixture(
            id = "TEST_A",
            title = "Test A: Baseline Existing Stock Range",
            description = "SA001 | Kumar | Class 12 | 100001 → 100050 (Qty 50 tags)",
            rawText = """
                FASTag INVENTORY MANIFEST - CENTRAL WAREHOUSE
                Subagent ID | Subagent Name | Tag Class | From Serial | To Serial
                SA001 | Kumar | Class 12 | 100001 | 100050
            """.trimIndent(),
            defaultFilename = "manifest_test_a_baseline.jpg"
        ),
        SampleReceiptFixture(
            id = "TEST_B",
            title = "Test B: Central Received Range",
            description = "SA002 | Ravi Sharma | Class 7 | 100051 → 100075 (Qty 25 tags)",
            rawText = """
                CENTRAL TOLL LOGISTICS DISPATCH NOTE
                Subagent ID | Subagent Name | Tag Class | From Serial | To Serial
                SA002 | Ravi Sharma | Class 7 | 100051 | 100075
            """.trimIndent(),
            defaultFilename = "central_dispatch_100051_100075.jpg"
        ),
        SampleReceiptFixture(
            id = "TEST_C",
            title = "Test C: Courier to EXEC-03",
            description = "Courier dispatch: 100001 → 100010 (Qty 10 tags to Suresh Patel)",
            rawText = """
                COURIER WAYBILL - EXEC-03 TRANSFER
                Subagent ID | Subagent Name | Tag Class | From Serial | To Serial
                SA001 | Kumar | Class 12 | 100001 | 100010
            """.trimIndent(),
            defaultFilename = "waybill_exec03_courier.jpg"
        ),
        SampleReceiptFixture(
            id = "TEST_D",
            title = "Test D: Tag Assignment at Toll Plaza",
            description = "Customer assignment: 100001 → 100005 (Qty 5 tags assigned)",
            rawText = """
                CUSTOMER FASTag ASSIGNMENT FORM
                Subagent ID | Subagent Name | Tag Class | From Serial | To Serial
                SA001 | Kumar | Class 12 | 100001 | 100005
            """.trimIndent(),
            defaultFilename = "assignment_form_plaza01.jpg"
        ),
        SampleReceiptFixture(
            id = "MULTI_TAG",
            title = "Multi-Tag Table Invoice",
            description = "Individual tags with distinct classes and serials",
            rawText = """
                FASTag BATCH INVOICE
                Subagent ID | Subagent Name | Tag Class | Tag Serial Number
                SA001 | Kumar | Class 12 | TAG001
                SA001 | Kumar | Class 12 | TAG002
                SA002 | Ravi | Class 7 | TAG003
                SA002 | Ravi | Class 7 | TAG004
                SA003 | Suresh | Commercial | TAG005
            """.trimIndent(),
            defaultFilename = "batch_invoice_multi.jpg"
        )
    )
}
