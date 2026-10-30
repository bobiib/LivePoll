db.polls.insertOne(
    {
        "_id" : "user_id (fk)",
        "name" : "SampleName",
        "questions" : [
            {
                "question": "Your question here?",
                "type" : "multi-choice",
                "answer": null,
                "options": [
                    "Option 1",
                    "Option 2",
                    "Option 3"
                ]
            },
            {
                "question": "Your question here?",
                "type": "single-choice",
                "answer": null,
                "options": [
                    "Option 1",
                    "Option 2",
                    "Option 3"
                ]
            },
            {
                "question": "Your question here?",
                "type": "text",
                "answer": null
            }
        ]
    }
)