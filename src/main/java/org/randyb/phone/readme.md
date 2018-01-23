## Meeting Notifications via telephone

Web app for scheduling a daily phone call as a meeting reminder. Used by multiple scrum teams for daily standup meetings. 

I built this to reduce the time spent waiting for all team members to arrive before scrum meetings could start. It was particularly useful in work environments where more conventional reminder systems were less reliable (e.g, multiple workstations and you're not using the one with your calendar at the time, cell phones prohibited, etc).

Uses Quartz library with PostgreSQL for scheduling, tropo.com REST APIs for telephony, and JQuery for the [UI](https://github.com/randyb/hobby-apps/blob/master/src/main/resources/scrum/index.html).

![](http://randyb.org/scrum.png) 
