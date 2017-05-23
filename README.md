# Webhook transform
Transformes messages from Splunk, like
```json
{
  "app": "SVV_vegkart",
  "results_link": "http://svvploggsh20.vegvesen.no:8443/app/SVV_vegkart/search?q=%7Cloadjob%20rt_scheduler__marvli_U1ZWX3ZlZ2thcnQ__TGest_at_1495187518_18226.795_792DCE67-808E-4AE2-B026-0EE345D04AD5%20%7C%20head%201%20%7C%20tail%201&earliest=0&latest=now",
  "result": {
    "app": "",
    "status": "",
    "_raw": "2017-05-19 12:19:04,015 TRANSID=\"031709\" SEVERITY=\"WARN\" ORIGIN=\"AreaCache\" MESSAGE=\"Unknown contract: 423369244\"",
    "sourcetype": "vegkart-api-log",
    "product": "",
    "date_mday": "19",
    "splunk_server": "svvploggis10.vegvesen.no",
    "date_minute": "19",
    "hostname": "",
    "_confstr": "source::/data/base-vegkart-01/logs/api-v1.log|host::svvpvegkartw01|vegkart-api-log",
    "_time": "1495189144.015",
    "_indextime": "1495189144",
    "tag": "",
    "object_category": "",
    "date_second": "4",
    "tag::app": "",
    "timestartpos": "0",
    "host": "svvpvegkartw01",
    "eventtype": "et_systemname",
    "vendor": "",
    "idx": "",
    "_serial": "11560",
    "TRANSID": "031709",
    "date_zone": "local",
    "_eventtype_color": "none",
    "MESSAGE": "Unknown contract: 423369244",
    "ORIGIN": "AreaCache",
    "_kv": "1",
    "_subsecond": ".015",
    "range": "",
    "date_year": "2017",
    "date_hour": "12",
    "SEVERITY": "WARN",
    "enabled": "",
    "source": "/data/base-vegkart-01/logs/api-v1.log",
    "_si": [
      "svvploggis10.vegvesen.no",
      "svvpvegkart"
    ],
    "_sourcetype": "vegkart-api-log",
    "punct": "--_::,_=\"\"_=\"\"_=\"\"_=\"_:_\"",
    "linecount": "1",
    "date_wday": "friday",
    "timeendpos": "24",
    "user_type": "",
    "change_type": "",
    "tag::eventtype": "",
    "index": "svvpvegkart",
    "date_month": "may"
  },
  "search_name": "TGest",
  "owner": "marvli",
  "sid": "rt_scheduler__marvli_U1ZWX3ZlZ2thcnQ__TGest_at_1495187518_18226.795_792DCE67-808E-4AE2-B026-0EE345D04AD5"
}
```
to
```
payload={"text": "## Error on svvpvegkartw01!
AreaCache: Unknown contract: 423369244
Source: /data/base-vegkart-01/logs/api-v1.log"}
```
