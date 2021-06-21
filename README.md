# Kafka Workshop Modulo03 Lab 1

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
sudo echo "127.0.0.1 broker0.io" >> /etc/hosts
```

4. Descargar utilidades kafka:

```bash
$ curl "https://downloads.apache.org/kafka/2.8.0/kafka_2.13-2.8.0.tgz" -o kafka.tgz
$ tar -zxvf kafka.tgz
$ mv kafka_2.13-2.8.0 kafka
```

5. Obtener certificado TLS

```bash
$ kubectl -n kafka get secret lab-cluster-ca-cert -o jsonpath='{.data.ca\.crt}' | base64 -d > ca.crt

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
