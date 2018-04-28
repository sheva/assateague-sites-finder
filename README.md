# Assateague Sites Finder
Helper code to find available sites on Assateague National Seashore. 
If you find this project than you are familiar with difficulty to find available camping spot at the summer. You should refer to [ASSATEAGUE ISLAND NATIONAL SEASHORE CAMPGROUND, MD](https://www.recreation.gov/camping/assateague-island-national-seashore-campground/r/campgroundDetails.do?contractCode=NRSO&parkId=70989) quite often to find out free site. So I automated this process.
I trace available sites by desired criteria. If free spot found, email with sites availability info is sent to specified recipients. 

## Setting search criteria

Search parameters are set in [app.properties](https://github.com/sheva/assateague-sites-finder/blob/master/src/main/resources/app.properties) file

1. Loop groups are represented in **search.loop.names** property, seprated by semicolon and could be *Bayside Loop A; Bayside Loop B; Bayside Loop C; Equestrian Non-Electric; Oceanside Group Sites; Oceanside Loop 1; Oceanside Loop 2; Oceanside Walk In 42-44, 51-82; Oceanside Walk In 83-104; Oceanside Walk-In 45-50*
```
search.loop.names=Oceanside Group Sites; Oceanside Loop 1; Oceanside Loop 2
```
2. Desired days of week to stay stored in **search.days.of.week** property, separated by semicolon. Possible optons: *MON, TUE, WED, THU, FRI, SAT, SUN*.
```
search.days.of.week=FRI; SAT; SUN
```
3. Min length of stay (in days)
```
search.length.of.stay=2
```
4. Start of period to search in (format *yyyy-MM-dd*)
```
search.start.date=2018-06-01
```
5. End of period to search in (format *yyyy-MM-dd*)
```
search.stop.date=2018-09-01
```

### Mail configuration

If you want to send email notifications (I am sure you want :) ) you should configure properly mail section in [app.properties](https://github.com/sheva/assateague-sites-finder/blob/master/src/main/resources/app.properties). Replace placeholders with Gmail appropriate account credentials. Also do not forget to mention recipients email addresses in **mail.to.list** property, separated by semicolon.
```
mail.from.username=%%FROM_EMAIL_ADDRESS%%
mail.from.password=%%FROM_PASSWORD%%
mail.to.list=%%TO_EMAIL_ADDRESSES%%
```
