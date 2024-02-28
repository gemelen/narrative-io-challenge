# Narrative I/O tech assignment

## Solution

### Design

Solution implemented as a HTTP server with a required set of endpoints, backed by an http4s/Ember engine,
 receiving requests to submit event data or to request statistics within hour of the provided timestamp.

Data submitted for storage passes minimalistic validation:
- is a timestamp positive integer number?
- is a user id positive integer number?
- is an event of a know type?

NB: *If a data record isn't valid according to these rules, it is silently dropped, without signalling to the submitter.*

Further, data is stored into QuestDB-managed datastore, with a table configured to match a desired submit/analyse patterns:
- records are data points in a time series, partitioned by hour, event type is a dictionary (auto-managed by DB engine)
- data for a summary request is combined as a result of three distinctive queries, asking DB for an user count, a clicks count and an impressions count

### Tools I used

- embedded [QuestDB](https://questdb.io) / [Database of Databases/QuestDB](https://dbdb.io/db/questdb/revisions/10)
- Scala 3
- Typelevel stack: Cats / Cats-Effect / http4s / fs2
- sbt

### How to run

It is assumed that you have installed JDK, sbt and Git on your local machine.

- Clone the repository
- Change directory to the root of the cloned repository
- Execute `sbt assembly` to create a JAR file that you may run with `java -jar ./target/scala-3.3.3/narrativeio-challenge.jar` or
- Execute `sbt run` to launch the application with the help of sbt itself

### Exemplar data and behaviour

Assumed that application is launched successfully (it should create a temporary directory in `/tmp`) and that you have the `curl` installed.

1. Input valid data
```
curl -X POST http://localhost:8888/analytics\?timestamp=946688461000\&user=1\&event=impression
curl -X POST http://localhost:8888/analytics\?timestamp=946688462000\&user=2\&event=impression
curl -X POST http://localhost:8888/analytics\?timestamp=946688462000\&user=1\&event=click
curl -X POST http://localhost:8888/analytics\?timestamp=946688463000\&user=3\&event=impression
curl -X POST http://localhost:8888/analytics\?timestamp=946688463000\&user=2\&event=click
curl -X POST http://localhost:8888/analytics\?timestamp=946688464000\&user=4\&event=impression
curl -X POST http://localhost:8888/analytics\?timestamp=946688464000\&user=3\&event=click
curl -X POST http://localhost:8888/analytics\?timestamp=946688465000\&user=4\&event=click
```
observation 
```
...
2024-03-06T02:19:55.560664Z I i.q.c.p.WriterPool << [table=`pixel`, thread=24]
2024-03-06T02:19:55.575394Z I i.q.g.SqlCompilerImpl parse [fd=-1, thread=26, q=insert into pixel (ts, user_id, event) values (CAST(946688465000 AS DATE), 4, 'click');]
2024-03-06T02:19:55.576774Z I i.q.c.p.WriterPool >> [table=`pixel`, thread=26]
...
```
2. Request summary with timestamp NOT within the data range
```
curl http://localhost:8888/analytics\?timestamp=-1
```

response
```
unique_users,0
clicks,0
impressions,0
```
3. Request summary with timestamp within the data range
```
curl http://localhost:8888/analytics\?timestamp=946691942000
```
response
```
unique_users,4
clicks,4
impressions,4
```
4. Input invalid data
```
curl -X POST http://localhost:8888/analytics\?timestamp=0\&user=-1\&event=something
```
observation
```
23:20:40.940 [io-compute-0] WARN net.gemelen.dev.narrativeio.controllers.Analytics -- Input data was dropped due to the validation error. Input is: (timestamp = 0, user_id = -1, event = something). Errors are: NonEmptyList(User Id doesn't conform expectations, Event is of an unknown type)
```
5. Input more data and check for the difference
```
curl -X POST http://localhost:8888/analytics\?timestamp=946688462000\&user=2\&event=click
curl http://localhost:8888/analytics\?timestamp=946691942000
```
response
```
unique_users,4
clicks,5
impressions,4
```


## Original text of the assignment

As a part of integrating with our partners, Narrative supports collecting
data on website visitors and returning some basic analytics on those
visitors. The goal of this task is to implement a basic endpoint for this
use case. It should accept the following over HTTP:

`POST /analytics?timestamp={millis_since_epoch}&user={user_id}&event={click|impression}`

`GET /analytics?timestamp={millis_since_epoch}`

When the POST request is made, a 204 is returned to the client with an
empty response. We simply side-effect and track the event in our data store.
When the GET request is made, we return information in the following
format to the client, for the hour (assuming GMT time zone) of the
requested timestamp:
```
unique_users,{number_of_unique_usernames}
clicks,{number_of_clicks}
impressions,{number_of_impressions}
```

It is worth noting that the traffic pattern is typical of time series
data. The service will receive many more GET requests (~95%) for the
current hour than for past hours (~5%). The same applies for POST requests.

Please ensure that the code in the submission is fully functional on a
local machine, and include instructions for building and running
it. Although it should still pass muster in code review, it is fine for the
code to not be completely production ready in the submission. 
For example, using local storage like in-memory H2 instead of dedicated MySQL is OK. As
a guide for design decisions, treat this exercise as the initial prototype
of an MVP that will need to be productionalized and scaled out in the
future, and be prepared for follow-up discussion on how that would look.
