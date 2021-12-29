### Metrics collected

Hand started cluster. First 200000 requests are done with a warm up test (no actual result validation, only raw throughput). Test was performed with parallelism coeficient equal to 2, two games, two service. Values below represent average for both services (difference was negligible).

| Counter                    | Blind Write | Check And Set | Pessimistic Lock |
| :------------------------- | :---------- | :------------ | ---------------- |
| cache-hit-counter          | 1021084     | 1023848       | 1002876          |
| cache-miss-counter         | 1090        | 1784          | 1241             |
| cache-write-failes-counter | 0           | 1200          | 618              |
| request-counter            | 1022175     | 1024433       | 1003499          |

#### Blind Write

| Metric             | min   | max     | mean  | stddev | median | p75   | p95   | p98   | p99   |
| :----------------- | :---- | :------ | :---- | :----- | :----- | :---- | :---- | :---- | :---- |
| cache-search-timer | 0.017 | 0.125   | 0.033 | 0.009  | 0.032  | 0.036 | 0.047 | 0.054 | 0.070 |
| cache-write-timer  | 0.039 | 2.457   | 0.076 | 0.066  | 0.071  | 0.081 | 0.105 | 0.127 | 0.158 |
| db-search-timer    | 0.177 | 287.891 | 0.585 | 5.852  | 0.452  | 0.514 | 0.665 | 0.766 | 0.880 |
| response-timer     | 0.020 | 0.460   | 0.034 | 0.019  | 0.032  | 0.037 | 0.048 | 0.055 | 0.063 |

#### Check And Set

| Metric             | min   | max     | mean  | stddev | median | p75   | p95   | p98   | p99   |
| :----------------- | :---- | :------ | :---- | :----- | :----- | :---- | :---- | :---- | :---- |
| cache-search-timer | 0.030 | 0.316   | 0.055 | 0.015  | 0.053  | 0.060 | 0.076 | 0.086 | 0.100 |
| cache-write-timer  | 0.045 | 1.762   | 0.091 | 0.041  | 0.087  | 0.100 | 0.130 | 0.152 | 0.182 |
| db-search-timer    | 0.124 | 141.330 | 0.359 | 1.592  | 0.360  | 0.452 | 0.537 | 0.591 | 0.648 |
| response-timer     | 0.046 | 2.046   | 0.079 | 0.039  | 0.076  | 0.086 | 0.104 | 0.115 | 0.138 |

#### Pessimistic Lock

| Metric             | min   | max    | mean  | stddev | median | p75   | p95   | p98   | p99   |
| :----------------- | :---- | :----- | :---- | :----- | :----- | :---- | :---- | :---- | :---- |
| cache-search-timer | 0.030 | 0.512  | 0.055 | 0.022  | 0.053  | 0.059 | 0.074 | 0.090 | 0.113 |
| cache-write-timer  | 0.056 | 10.337 | 0.105 | 0.133  | 0.099  | 0.112 | 0.144 | 0.168 | 0.215 |
| db-search-timer    | 0.173 | 2.924  | 0.445 | 0.124  | 0.443  | 0.491 | 0.616 | 0.697 | 0.810 |
| response-timer     | 0.046 | 2.238  | 0.080 | 0.059  | 0.076  | 0.086 | 0.103 | 0.121 | 0.141 |
