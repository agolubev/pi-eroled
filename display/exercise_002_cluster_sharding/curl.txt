curl -d '{"amount":1}' -H "Content-Type: application/json" -X POST http://192.168.1.100:8080/user/Alex
curl -d '{"amount":2}' -H "Content-Type: application/json" -X POST http://192.168.1.100:8080/user/Max
curl -d '{"amount":3}' -H "Content-Type: application/json" -X POST http://192.168.1.101:8080/user/Peter
curl -d '{"amount":4}' -H "Content-Type: application/json" -X POST http://192.168.1.101:8080/user/Vlad


curl http://192.168.1.100:8080/user/Peter

curl http://node-0:8558/cluster/members/ | jq

curl -d 'operation=DOWN' -X PUT http://192.168.1.100:8558/cluster/members/akka-oled@192.168.1.102:2550