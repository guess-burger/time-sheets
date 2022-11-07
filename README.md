# Time-Sheets

More Adventures into mundanity building [timesheets](http://guess-burger.github.io/timesheets/)!!!

This project extends on what [elm-time](https://github.com/guess-burger/elm-time) attempted
to do by supporting multiple days worth of timesheet goodness while being powered
by ClojureScript this time. [Try it out!](http://guess-burger.github.io/timesheets/).

This project provides a simple site to turn human-friendly timesheet tracking like
```
# MON
0900 Task A
1200 Lunch
1230 Task A
1400 Meeting A
1445 Task B
1700 home

# TUE
0900 Task A
; comment
1030 Meeting B
1045 Task B
1200 Lunch
1230 Task C
1600 Task B 
1700 home
```

into the kind of stuff JIRA and Salesforce want you to track your time as, like

|           | MON  | TUE  |
|-----------|------|------|
| Lunch     | 0.5  | 0.5  |
| Meeting A | 0.75 | 0    |
| Meeting B | 0    | 0.25 |
| Task A    | 4.5  | 1.5  |
| Task B    | 2.25 | 2.25 |
| Task C    | 0    | 3.5  |
| Total     | 7.5  | 7.5  |

It has a few creature comforts, notice how Lunch isn't contributing to the total.
That's because it allows you to ignore tasks you wouldn't usually log (with a colour highlight).
It also allows arbitrary headers of your choice (days of thr week, month, whatever) by prefixing lines with `#`.
As well as comments (for extra notes) by prefixing lines with `;`.


## Building

Firstly, install some dependencies via npm or Yarn like

    $ yarn install

Then run the app in dev mode

    $ clj -M:shadow-cljs watch app

This will use [shadow-cljs](https://shadow-cljs.github.io/docs/UsersGuide.html) to 
serve the app on `localhost:8020` and watch for changes.

To make a release, run

    $ clj -M:shadow-cljs release app


## License

Copyright © 2022 guess-burger

Distributed under the Eclipse Public License version 1.0.
