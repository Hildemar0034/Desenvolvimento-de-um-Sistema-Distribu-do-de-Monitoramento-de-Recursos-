Versão 2: Integração completa (RPC sobre TCP simples), heartbeat->eleição automática, snapshot via RPC, controle de acesso por token.
Compile: mvn package
Run example (3 nós locais):
  java -cp target/monitoramento-distribuido-2.0-SNAPSHOT.jar br.com.monitoramento.Main 1 5000 "2:5001,3:5002"
  java -cp target/monitoramento-distribuido-2.0-SNAPSHOT.jar br.com.monitoramento.Main 2 5001 "1:5000,3:5002"
  java -cp target/monitoramento-distribuido-2.0-SNAPSHOT.jar br.com.monitoramento.Main 3 5002 "1:5000,2:5001"
