# Kafka Workshop Modulo06 Lab 1

Basado en:

- [Vinsguru Architecturla Patterns SAGA Coreography](https://github.com/vinsguru/vinsguru-blog-code-samples/tree/master/architectural-pattern/saga-choreography)
- [Vinsguru Architecturla Patterns SAGA Orchestration](https://github.com/vinsguru/vinsguru-blog-code-samples/tree/master/architectural-pattern/saga-orchestration)

> Una recomendación es trabajar en Linux y en caso de no tener recursos suficientes, se puede crear una cuenta
gratis por USD$100 en [Digital Ocean's](https://try.digitalocean.com/freetrialoffer) o en [Linode](https://www.linode.com/lp/free-credit-100) y disfrutar de una máquina virtual Ubuntu de 4 cores
y 8GBG RAM. Para trabajo local, se puede montar la carpeta de trabajo de la VM usando [SSHFS](https://www.digitalocean.com/community/tutorials/how-to-use-sshfs-to-mount-remote-file-systems-over-ssh).

Para configurar el laboratorio donde se van a desarrollar los ejercicios del workshop,
se debe configurar un ambiente con los siguientes requerimientos:

** Alternativa 1 **

1. Docker Desktop (Docker CE en Linux)
2. [Kubernetes in Docker](https://kind.sigs.k8s.io/)

** Alternativa 2 **

1. [Minikube](https://minikube.sigs.k8s.io/docs/start/)

La alternativa 1, permite tener más de un nodo en k8s y además, en el caso de Windows y Mac, permite usar Docker 
con la misma VM y en el caso de Linux, no necesita VM para k8s. Adicionalmente se necesitan las siguientes
herramientas:

1. kubectl
2. Java JDK 15
3. [krew](https://krew.sigs.k8s.io/)
4. [Skaffold](https://skaffold.dev/)

## Instalación en Ubuntu

1. Actualizar apt-get

```bash 
$ sudo apt-get update
```

2. Instalar dependencias de Docker 

```bash 
$ sudo apt-get install     apt-transport-https     ca-certificates     curl     gnupg     lsb-release
```

3. Registrar repositorio de Docker

```bash 
$ echo   "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu \
 $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
```

4. Actualizar apt-get

```bash 
$ sudo apt-get update
```

5. Instalar Docker CE

```bash 
$ sudo apt-get install docker-ce docker-ce-cli containerd.io
```

6. Instalar kind
   
```
$ curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.11.1/kind-linux-amd64
$ chmod +x ./kind
$ sudo mv ./kind /usr/bin/kind
```

7. Crear cluster k8s
   
```bash
kind create cluster --name strimzi-lab
```

8. Instalar kubectl

```bash
$ sudo apt-get install -y apt-transport-https ca-certificates curl
$ sudo curl -fsSLo /usr/share/keyrings/kubernetes-archive-keyring.gpg https://packages.cloud.google.com/apt/doc/apt-key.gpg
$ echo "deb [signed-by=/usr/share/keyrings/kubernetes-archive-keyring.gpg] https://apt.kubernetes.io/ kubernetes-xenial main" | sudo tee /etc/apt/sources.list.d/kubernetes.list
$ sudo apt-get update
$ sudo apt-get install -y kubectl
```

9. Instalar [krew](https://krew.sigs.k8s.io/) / **NO USAR EN PRODUCCION**/

```bash
$ (
  set -x; cd "$(mktemp -d)" &&
  OS="$(uname | tr '[:upper:]' '[:lower:]')" &&
  ARCH="$(uname -m | sed -e 's/x86_64/amd64/' -e 's/\(arm\)\(64\)\?.*/\1\2/' -e 's/aarch64$/arm64/')" &&
  curl -fsSLO "https://github.com/kubernetes-sigs/krew/releases/latest/download/krew.tar.gz" &&
  tar zxvf krew.tar.gz &&
  KREW=./krew-"${OS}_${ARCH}" &&
  "$KREW" install krew
)

$ echo 'export PATH="${KREW_ROOT:-$HOME/.krew}/bin:$PATH"' > ~/.bashrc
$ export PATH="${KREW_ROOT:-$HOME/.krew}/bin:$PATH"
```

9. Instalar kubectx y kubens

```bash
$ kubectl krew install ctx
$ kubectl krew install ns
```

10. Instalar [Skaffold](https://skaffold.dev/)

```bash
$ curl -Lo skaffold https://storage.googleapis.com/skaffold/releases/latest/skaffold-linux-amd64 && \
$ sudo install skaffold /usr/local/bin/
```

## Kafka

Kafka es relativamente complejo de instalar y configurar en forma adecuada. Para simplificar la instalación y
la administración de Kafka en k8s, vamos a utilizar una distribución de Kafka basada en un operador de k8s
perteneciente a la [Cloud Native Computing Foundation](https://www.cncf.io/), [Strimzi](https://strimzi.io/).

Para instalar ejecutar:

```bash
$ kubectl apply -f k8s/strimzi
```

## Ejecutar aplicación en k8s

Para ejecutar la aplicación en k8s en modo desarrollo ejecutar:

```bash
$ skaffold dev
```

## Exponer servicios de k8s

1. Configurar ingress en nodeport (ssl passthrough configurado):

```bash
$ kubectl apply -f k8s/nginx/deploy.yaml
```

2. Exponer nodeport en Docker:

```bash
for port in 80 443
do
    node_port=$(kubectl get service -n ingress-nginx ingress-nginx-controller -o=jsonpath="{.spec.ports[?(@.port == ${port})].nodePort}")

    docker run -d --name strimzi-lab-kind-proxy-${port} \
      --publish 127.0.0.1:${port}:${port} \
      --network=kind \
      --link strimzi-lab-control-plane:target \
      alpine/socat -dd \
      tcp-listen:${port},fork,reuseaddr tcp-connect:target:${node_port}
done
```

3. Agregar entradas de DNS locales para el bootstrap y el broker:

```bash
sudo echo "127.0.0.1 bootstrap.io" >> /etc/hosts
sudo echo "127.0.0.1 broker-0.io" >> /etc/hosts
```

4. Descargar utilidades kafka:

```bash
$ curl "https://downloads.apache.org/kafka/2.8.0/kafka_2.13-2.8.0.tgz" -o kafka.tgz
$ tar -zxvf kafka.tgz
$ mv kafka_2.13-2.8.0 kafka
```

5. Obtener certificado TLS

```bash
$ kubectl get secret lab-cluster-ca-cert -o jsonpath='{.data.ca\.p12}' | base64 -d > client.p12
$ kubectl get secret lab-cluster-ca-cert -o jsonpath='{.data.ca\.password}' | base64 -d > client.password
$ keytool -importkeystore -srckeystore client.p12 -destkeystore kafka.client.keystore.jks -srcstoretype pkcs12 -alias lab -storepass $(cat client.password) -noprompt
$ keytool -importkeystore -srckeystore client.p12 -destkeystore kafka.client.keystore.jks -srcstoretype pkcs12
$ sudo mkdir -p /var/private/ssl
$ sudo cp kafka.client.*.jks /var/private/ssl/
```

Crear archivo client.properties:

```conf
security.protocol=SSL
ssl.keystore.location=/var/private/ssl/kafka.client.keystore.jks
ssl.keystore.password=<keystore password>
ssl.key.password=<pkcs12 password>
ssl.truststore.location=/var/private/ssl/kafka.client.truststore.jks
ssl.truststore.password=<truststore password>
```

Y ejecutar:

```bash
$ chmod 0600 client.properties
```

6. Conectar el productor:
  
```bash
$ kafka/bin/kafka-console-producer.sh --broker-list producer.io:443 -topic test --producer.config client.properties
```

7. Abrir otro terminal y conectar el consumidor:

```bash
$ kafka/bin/kafka-console-consumer.sh bin/kafka-console-consumer --bootstrap-server bootstrap.io:443 --topic test --consumer.config client.properties --from-beginning
```

Ahora puedes escribir mensaje en el productor y ver como los recibe el consumidor.

## Ejecutar aplicación productor/cliente

El proyecto tiene una aplicación que contiene un producto y un consumidor para kafka basado en Spring Boot. Para ejecutar la aplicación (sin skaffold), se debe correr:

```bash
KEY_PASSWORD=$(cat ~/client.password) BOOTSTRAP_ADDRESS="bootstrap.io:443" TRUSTED_STORE_LOCATION="/var/private/ssl/kafka.client.truststore.jks" TRUSTED_STORE_PASSWORD=password KEY_STORE_LOCATION="/var/private/ssl/kafka.client.keystore.jks" KEY_STORE_PASSWORD=password GROUP_ID="modulo3lab1" MESSAGE_QTY=10 ./mvnw spring-boot:run
```

Donde ```~/client.password ```es la localización del password del certificado para kafka.

## SAGA Choreografy

En el presente ejemplo, vamos a desplegar la aplicación en el cluster de Kubernetes, por lo tanto, no es necesaria
seguridad TLS.

> Considerar que dentro dle cluster la comunicación es encriptada y la autenticación no es necesaria. Si podría ser necesaria la autorización que está cubierta por Strimzi y por Kafka.

Para ejejcutar en Java 16 es necesario permitir el acceso ilegal:

```bash
$ JAVA_TOOL_OPTIONS="--illegal-access=permit" ./mvnw install
$ ./mvnw spring-boot:run -pl inventory-service &
$ ./mvnw spring-boot:run -pl order-service &
$ ./mvnw spring-boot:run -pl payment-service &
```

Es un proyecto multi-módulo, por eso es necesario iniciar los 3 servicios en forma independiente. Para correr en K8s, primero debemos generar el Dockerfile:

```dockerfile
FROM openjdk:16-jdk-slim as build

ARG MODULE
ARG SERVICE

WORKDIR /app

COPY ./${MODULE}/. ./

RUN JAVA_TOOL_OPTIONS="--illegal-access=permit" ./mvnw package

RUN  cp $SERVICE/target/*.jar $SERVICE/target/app.jar

FROM openjdk:16-jdk-slim as production

ARG SERVICE

RUN useradd -u 1001 app

WORKDIR /app

COPY --from=build /app/$SERVICE/target/app.jar /app/app.jar

USER app

ENTRYPOINT ["java","-jar","/app/app.jar"]
```

Luego un manifesto para desplegar los servicios:

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: inventory-service
  namespace: kafka
spec:
  containers:
  - name: inventory-service
    image: inventory-service
     resources:
      limits:
        memory: "200Mi"
        cpu: "1"
      requests:
        memory: "100Mi"
        cpu: "0.5"
---
apiVersion: v1
kind: Pod
metadata:
  name: payment-service
  namespace: kafka
spec:
  containers:
  - name: payment-service
    image: payment-service
    resources:
      limits:
        memory: "200Mi"
        cpu: "1"
      requests:
        memory: "100Mi"
        cpu: "0.5"
---
apiVersion: v1
kind: Pod
metadata:
  name: order-service
  namespace: kafka
  labels:
    app: order-service
spec:
  containers:
  - name: order-service
    image: order-service
     resources:
      limits:
        memory: "200Mi"
        cpu: "1"
      requests:
        memory: "100Mi"
        cpu: "0.5"
    ports:
        - name: web
          containerPort: 8080
          protocol: TCP
---
apiVersion: v1
kind: Service
metadata:
  name: order-service
spec:
  selector:
    app: order-service
  ports:
    - protocol: TCP
      port: 8080
      targetPort: 8080
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: order-service
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - http:
      paths:
      - path: /order-service
        pathType: Prefix
        backend:
          service:
            name: order-service
            port:
              number: 8080
```

Finalmente, configurar skaffold para desplegar los servicios:

```yaml
apiVersion: skaffold/v2beta17
kind: Config
metadata:
  name: kafka-workshop-modulo3-lab1
build:
  artifacts:
  - image: inventory-service
    docker:
      dockerfile: Dockerfile
      buildArgs:
        MODULE: 'saga-choreography'
        SERVICE: 'inventory-service'
  - image: order-service
    docker:
      dockerfile: Dockerfile
      buildArgs:
        MODULE: 'saga-choreography'
        SERVICE: 'order-service'
  - image: payment-service
    docker:
      dockerfile: Dockerfile
      buildArgs:
        MODULE: 'saga-choreography'
        SERVICE: 'payment-service'
deploy:
  kubectl:
    manifests:
    - k8s/101-app.yaml
```




