pipeline {
    agent any

    environment {
        // Image tags — bump version on each release
        AUTH_IMAGE     = 'auth-service:v1'
        GATEWAY_IMAGE  = 'api-gateway:v1'
        NOTIF_IMAGE    = 'notification-service:v1'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Auth Service') {
            steps {
                dir('auth-service') {
                    sh 'docker build -t $AUTH_IMAGE .'
                }
            }
        }

        stage('Build API Gateway') {
            steps {
                dir('api-gateway') {
                    sh 'docker build -t $GATEWAY_IMAGE .'
                }
            }
        }

        stage('Build Notification Service') {
            steps {
                dir('notification-service') {
                    sh 'docker build -t $NOTIF_IMAGE .'
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh 'kubectl apply -f k8s.yml'
            }
        }

        stage('Restart Deployments') {
            steps {
                sh 'kubectl rollout restart deployment/auth-service'
                sh 'kubectl rollout restart deployment/api-gateway'
                sh 'kubectl rollout restart deployment/notification-service'
            }
        }

        stage('Wait for Rollout') {
            steps {
                sh 'kubectl rollout status deployment/postgres --timeout=60s'
                sh 'kubectl rollout status deployment/zookeeper --timeout=60s'
                sh 'kubectl rollout status deployment/kafka --timeout=90s'
                sh 'kubectl rollout status deployment/auth-service --timeout=120s'
                sh 'kubectl rollout status deployment/notification-service --timeout=120s'
                sh 'kubectl rollout status deployment/api-gateway --timeout=120s'
            }
        }

        stage('Verify') {
            steps {
                echo '====== PODS ======'
                sh 'kubectl get pods -o wide'
                echo '====== SERVICES ======'
                sh 'kubectl get services'
                echo '====== INGRESS ======'
                sh 'kubectl get ingress'
            }
        }

        stage('Smoke Test') {
            steps {
                script {
                    // Get the gateway NodePort URL
                    def gatewayUrl = sh(
                        script: "kubectl get service api-gateway -o jsonpath='{.spec.clusterIP}'",
                        returnStdout: true
                    ).trim()

                    echo "API Gateway ClusterIP: ${gatewayUrl}"
                    echo "Access via NodePort: http://<minikube-ip>:30080"

                    // Quick health check
                    sh """
                        kubectl run curl-test --rm -i --restart=Never \
                            --image=curlimages/curl:latest -- \
                            curl -s -o /dev/null -w '%{http_code}' \
                            http://api-gateway:8080/actuator/health || true
                    """
                }
            }
        }
    }

    post {
        success {
            echo '''
            =============================================
            ✅ DEPLOYMENT SUCCESSFUL
            =============================================
            Access the API Gateway:
              minikube service api-gateway --url
              OR
              http://myapp.local  (if Ingress + /etc/hosts configured)

            Test Registration:
              curl -X POST http://<URL>/auth/register \\
                -H "Content-Type: application/json" \\
                -d '{"username":"john","email":"john@test.com","password":"password123"}'

            Monitoring:
              Zipkin:      kubectl port-forward svc/zipkin 9411:9411
            =============================================
            '''
        }
        failure {
            echo '''
            =============================================
            ❌ DEPLOYMENT FAILED — Collecting diagnostics
            =============================================
            '''
            sh 'kubectl get pods || true'
            sh 'kubectl describe pods --selector=app=auth-service || true'
            sh 'kubectl logs -l app=auth-service --tail=50 || true'
            sh 'kubectl describe pods --selector=app=api-gateway || true'
            sh 'kubectl logs -l app=api-gateway --tail=50 || true'
            sh 'kubectl describe pods --selector=app=notification-service || true'
            sh 'kubectl logs -l app=notification-service --tail=50 || true'
        }
    }
}
