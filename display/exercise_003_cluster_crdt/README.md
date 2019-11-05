curl -d '{"status":"Up"}' -H "Content-Type: application/json" -X POST http://node-0:8080/status/NodeA
curl -d '{"status":"Up"}' -H "Content-Type: application/json" -X POST http://192.168.1.100:8080/status/NodeB
curl -d '{"status":"Up"}' -H "Content-Type: application/json" -X POST http://192.168.1.101:8080/status/NodeC
curl -d '{"status":"Down"}' -H "Content-Type: application/json" -X POST http://192.168.1.101:8080/status/NodeD


curl http://192.168.1.100:8080/user/Peter

curl http://node-0:8558/cluster/members/ | jq

curl -d 'operation=DOWN' -X PUT htt