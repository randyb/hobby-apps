## Steam Twitter Bot

[Twitter bot](https://twitter.com/randy_steam_bot) that tweets news about the video games.

Periodically scrapes and parses my Steam profile page, using the JSoup HTML parsing library, to get a list of games I that "follow". Then uses Steam REST APIs to fetch news items about them, and tweets them using the Twitter4J library.  