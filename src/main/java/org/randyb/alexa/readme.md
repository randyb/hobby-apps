## Toy "skills" for Amazon's Alexa service.

A collection of simple skills, implemented using a REST endpoint that Amazon calls when a user invokes them from Echo hardware. 

* **SillyWords** - Makes Alexa say a given number of random [interjections](https://developer.amazon.com/docs/custom-skills/speechcon-reference-interjections-english-us.html) (special words and phrases Alexa pronounces expressively). I [published this](https://www.amazon.com/randyb-org-SillyWords/dp/B07114RPKY) in the Alexa Skill store and received free Echo hardware from Amazon.
    > User: Tell SillyWords to say four silly words
    
    > Alexa: abracadabra! bam! kerboom! zoinks!

* **Overwatch** - Makes Alexa say some of my stats from the video game Overwatch. Consumes a 3rd party JSON API.
    > User: Ask Jinkies about my Overwatch stats.
    
    > Alexa: You are diamond tier with a skill rating of 2400 and win rate of 72 percent.

* **Food** - Teases our dog, Jinkies, who is very sensitive to words that sound similar to "hungry".

    > User: Ask Jinkies about food.
    
    > Alexa: Jinkies, are you ... hungover?
    
    > Jinkies: _[\*confused\*](http://randyb.org/jinkies.jpg)_

* **Door** - Tells me when my front door was last opened. Pulls the latest timestamp from a Mongo database, which gets logged to via an [IoT wifi sensor](http://supermechanical.com/twine/) attached to my front door.
    > User: Ask Jinkies about the door.
    
    > Alexa: The door was last opened at 4:32 pm.

---

_Note: Except for SillyWords, all skills use "Jinkies" as the invocation word. This made it easier to quickly experiment with new behaviors without having to register new skills through Amazon's developer portal._

    
 
