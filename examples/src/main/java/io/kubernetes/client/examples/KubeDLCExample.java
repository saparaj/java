package io.kubernetes.client.examples;

import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.Yaml;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * A simple example of how to use the Java API from an application outside a kubernetes cluster
 *
 * <p>Easiest way to run this: mvn exec:java
 * -Dexec.mainClass="io.kubernetes.client.examples.KubeConfigFileClientExample"
 *
 * <p>From inside $REPO_DIR/examples
 */
public class KubeDLCExample {
  private interface Handler {
    void handle(Object o) throws ApiException;
  }
  // Make a map that translates a Class object to a Handler
  private static final Map<Class, Handler> apiCallByClass = new HashMap<Class, Handler>();

  // public static init map
  static {
    apiCallByClass.put(
        V1Deployment.class,
        new Handler() {
          public void handle(Object o) throws ApiException {
            createDeployment((V1Deployment) o);
          }
        });
    apiCallByClass.put(
        ExtensionsV1beta1Ingress.class,
        new Handler() {
          public void handle(Object o) throws ApiException {
            createIngress((ExtensionsV1beta1Ingress) o);
          }
        });

    apiCallByClass.put(
        V1Service.class,
        new Handler() {
          public void handle(Object o) throws ApiException {
            createService((V1Service) o);
          }
        });
  }

  public static void main(String[] args) throws IOException, ApiException {

    setApiClient("/Users/saparaj/.kube/dlctest/config");

    System.out.println(
        "-----------------------------------------------------------------------------------");
    List<String> podNames = new ArrayList<String>();
    podNames = getListAllNamespaces();
    System.out.println(podNames);
    int ch;
    do {
      System.out.println(
          "-----------------------------------------------------------------------------------");
      System.out.println("Deployment Strategies");
      System.out.println("1. Initial Deployment");
      System.out.println("2. Rolling deployment");
      System.out.println("3. Blue-Green Deployment");
      System.out.println("4. Canary Deployment");
      System.out.println("5. Delete the deployment");
      System.out.println("6. Exit");
      Scanner sc = new Scanner(System.in);
      ch = Integer.parseInt(sc.nextLine());
      switch (ch) {
        case 1:
          initialDeployment("helloworld", "ocirsecret", "phx.ocir.io/ax022wvgmjpq/bluegreen:1.0.0");
          break;
        case 2:
          rollingDeployment("helloworld", "ocirsecret", "phx.ocir.io/ax022wvgmjpq/bluegreen:2.0.0");
          break;
        case 3:
          blueGreenDeployment(
              "helloworld", "ocirsecret", "phx.ocir.io/ax022wvgmjpq/bluegreen:1.0.0");
          System.out.println("Blue Version is deployed");
          blueGreenDeployment(
              "helloworld", "ocirsecret", "phx.ocir.io/ax022wvgmjpq/bluegreen:2.0.0");
          break;
        case 4:
          canaryDeployment(
              "helloworld", "ocirsecret", "phx.ocir.io/ax022wvgmjpq/bluegreen:2.0.0", "30");
          break;
        case 5:
          deleteResources("helloworld");
          break;
        case 6:
          System.out.println("Exited");
          break;
        default:
          System.out.println("invalid choice");
          break;
      }
    } while (ch != 6);
  }

  public static void setApiClient(String kubeConfigPath) throws IOException {

    // loading the out-of-cluster config, a kubeconfig from file-system
    ApiClient client =
        ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath))).build();
    Configuration.setDefaultApiClient(client);
  }

  // To get list of pods in all Namespaces
  public static List<String> getListAllNamespaces() throws IOException, ApiException {
    CoreV1Api api = new CoreV1Api();
    // invokes the CoreV1Api client
    System.out.println("List of Pods in All Namespaces");
    V1PodList list =
        api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null);
    List<String> podNames = new ArrayList<String>();
    for (V1Pod item : list.getItems()) {
      podNames.add(item.getMetadata().getName());
    }
    return podNames;
  }

  public static void createService(V1Service body) throws ApiException {
    V1Service createResult;
    CoreV1Api api = new CoreV1Api();
    String metaName = body.getMetadata().getName();
    try {
      createResult = api.readNamespacedService(metaName, "default", null, null, null);
      if (createResult.getStatus() != null) {
        System.out.println(createResult.getMetadata().getName() + " service already exists");
      }
    } catch (ApiException e) {
      createResult = api.createNamespacedService("default", body, null, null, null);
      System.out.println(createResult.getMetadata().getName() + " service is created");
    }
  }

  public static void createDeployment(V1Deployment body) throws ApiException {
    V1Deployment createResult;
    AppsV1Api api = new AppsV1Api();
    String metaName = body.getMetadata().getName();
    try {
      createResult = api.readNamespacedDeployment(metaName, "default", null, null, null);
      if (createResult.getStatus() != null) {
        System.out.println(createResult.getMetadata().getName() + " already exists");
      }
    } catch (ApiException e) {
      createResult = api.createNamespacedDeployment("default", body, null, null, null);
      System.out.println(createResult.getMetadata().getName() + " deployment is created");
    }
  }

  public static void checkDeploymentStatus(String deploymentName) throws ApiException {
    V1Deployment deploymentStatus;
    while (true) {
      AppsV1Api api = new AppsV1Api();
      deploymentStatus = api.readNamespacedDeploymentStatus(deploymentName, "default", null);
      Integer replicas = deploymentStatus.getStatus().getReplicas();
      Integer readyReplicas = deploymentStatus.getStatus().getReadyReplicas();
      if (replicas != null && readyReplicas != null && replicas == readyReplicas) break;
    }
    System.out.println(deploymentName + " successfully rolled out");
  }

  public static void createIngress(ExtensionsV1beta1Ingress body) throws ApiException {
    ExtensionsV1beta1Ingress createResult;
    ExtensionsV1beta1Api api = new ExtensionsV1beta1Api();
    String metaName = body.getMetadata().getName();
    try {
      createResult = api.readNamespacedIngress(metaName, "default", null, null, null);
      if (createResult.getStatus() != null) {
        System.out.println(createResult.getMetadata().getName() + " ingress already exists");
      }
    } catch (ApiException e) {
      createResult = api.createNamespacedIngress("default", body, null, null, null);
      System.out.println(createResult.getMetadata().getName() + " ingress is created");
    }
  }

  public static void patchDeployment(String deploymentName, String imagePath) throws ApiException {
    AppsV1Api api = new AppsV1Api();
    final String jsonPatchStr =
        "[{\"op\":\"replace\",\"path\":\"/spec/template/spec/containers/0/image\",\"value\":\""
            + imagePath
            + "\"}]";
    System.out.println(jsonPatchStr);
    V1Deployment createResult =
        api.patchNamespacedDeployment(
            deploymentName, "default", new V1Patch(jsonPatchStr), null, null, null, null);
    System.out.println(createResult.getMetadata().getName() + " deployment is patched");
  }

  public static void patchIngress(String ingressName, String serviceName) throws ApiException {
    ExtensionsV1beta1Api api = new ExtensionsV1beta1Api();
    final String jsonPatchStr =
        "[{\"op\":\"replace\",\"path\":\"/spec/rules/0/http/paths/0/backend/serviceName\",\"value\":\""
            + serviceName
            + "\"}]";
    ExtensionsV1beta1Ingress createResult =
        api.patchNamespacedIngress(
            ingressName, "default", new V1Patch(jsonPatchStr), null, null, null, null);
    System.out.println(createResult + " ingress is patched with" + serviceName);
  }

  public static void loadAllResources(String content) throws IOException, ApiException {
    Yaml yaml = new Yaml();
    List<Object> listObj = yaml.loadAll(content);
    for (Object obj : listObj) {
      Handler h = apiCallByClass.get(obj.getClass());
      if (h != null) h.handle(obj);
    }
  }

  public static void deleteResources(String deploymentName) throws ApiException {
    // delete deployment
    V1Status createResult =
        new AppsV1Api()
            .deleteNamespacedDeployment(
                deploymentName, "default", null, null, null, null, null, null);
    createResult =
        new ExtensionsV1beta1Api()
            .deleteNamespacedIngress(deploymentName, "default", null, null, null, null, null, null);
    createResult =
        new CoreV1Api()
            .deleteNamespacedService(deploymentName, "default", null, null, null, null, null, null);
    System.out.println(deploymentName + " is deleted");
  }

  public static String replaceYamlWithVariables(
      String templateName,
      String applicationName,
      String imagePath,
      String imagePullSecret,
      String percentCanary,
      String productionSlot)
      throws IOException, ApiException {
    Yaml yaml = new Yaml();
    Iterable<Object> iterable = yaml.getSnakeYaml().loadAll(new FileReader(templateName));
    List<Object> list = new ArrayList<Object>();
    for (Object object : iterable) {
      if (object != null) {
        list.add(object);
      }
    }
    String content = yaml.getSnakeYaml().dumpAll(list.iterator());
    content = content.replaceAll("\\{\\{deploymentName}}", applicationName);
    content = content.replaceAll("\\{\\{image}}", imagePath);
    content = content.replaceAll("\\{\\{imagePullSecret}}", imagePullSecret);
    content = content.replaceAll("\\{\\{percentCanary}}", percentCanary);
    content = content.replaceAll("\\{\\{productionSlot}}", productionSlot);
    return content;
  }

  public static void initialDeployment(
      String applicationName, String imagePullSecret, String imagePath)
      throws IOException, ApiException {
    // Deploy application based on AppName, ImagePath and ImagePullSecret
    loadAllResources(
        replaceYamlWithVariables(
            "loadAllResourcesTemplate.yml", applicationName, imagePath, imagePullSecret, "", ""));
    System.out.println(applicationName + " with " + imagePath + " is deployed");
  }

  public static void rollingDeployment(
      String applicationName, String imagePullSecret, String newImagePath)
      throws IOException, ApiException {
    String ch = "";
    do {
      System.out.println("Press Y if ready to deploy");
      Scanner sc = new Scanner(System.in);
      ch = sc.nextLine();
      if (ch.equalsIgnoreCase("Y")) {
        // Update the deployment with new image
        patchDeployment(applicationName, newImagePath);
        System.out.println(applicationName + " is updated with " + newImagePath);
        break;
      }
    } while (!ch.equalsIgnoreCase("Y"));
  }

  public static void blueGreenDeployment(
      String applicationName, String imagePullSecret, String imagePath)
      throws IOException, ApiException {
    // Get Current value of Production Slot
    ExtensionsV1beta1Ingress createResult;
    ExtensionsV1beta1Api api = new ExtensionsV1beta1Api();
    boolean isBlue = false;
    try {
      createResult = api.readNamespacedIngress(applicationName, "default", null, null, null);
      if (createResult.getStatus() != null) {
        List<ExtensionsV1beta1IngressRule> obj = createResult.getSpec().getRules();
        String serviceName = obj.get(0).getHttp().getPaths().get(0).getBackend().getServiceName();
        isBlue = serviceName.contains("blue");
        if (isBlue) {
          loadAllResources(
              replaceYamlWithVariables(
                  "deployment-green.yml", "helloworld", imagePath, imagePullSecret, "", "green"));
        } else {
          loadAllResources(
              replaceYamlWithVariables(
                  "deployment-blue.yml", "helloworld", imagePath, imagePullSecret, "", "blue"));
        }
      }
      String ch = "";
      do {
        System.out.println("Press Y if ready to deploy");
        Scanner sc = new Scanner(System.in);
        ch = sc.nextLine();
        if (ch.equalsIgnoreCase("Y")) {
          if (isBlue) {
            patchIngress(applicationName, applicationName + "-green");
          } else {
            patchIngress(applicationName, applicationName + "-blue");
          }
        }
      } while (!ch.equalsIgnoreCase("Y"));

    } catch (ApiException e) {
      loadAllResources(
          replaceYamlWithVariables(
              "deployment-blue.yml", "helloworld", imagePath, imagePullSecret, "", "blue"));
      loadAllResources(
          replaceYamlWithVariables("ingress-bluegreen.yml", "helloworld", "", "", "", "blue"));
    }
  }

  public static void canaryDeployment(
      String applicationName, String imagePullSecret, String newImagePath, String canaryWt)
      throws IOException, ApiException {
    // Deploy Canary Application
    loadAllResources(
        replaceYamlWithVariables(
            "deployment-canary.yml", applicationName, newImagePath, imagePullSecret, "", ""));
    System.out.println(applicationName + " Canary with " + newImagePath + " is deployed");

    // Deploy Canary Ingress
    loadAllResources(
        replaceYamlWithVariables(
            "ingress-canary.yml", applicationName, newImagePath, imagePullSecret, canaryWt, ""));
    System.out.println(
        applicationName + " Ingress is deployed with Canary Weight " + canaryWt + "%");

    // Validation in progress
    String ch = "";
    do {
      System.out.println("Press Y if ready to deploy");
      Scanner sc = new Scanner(System.in);
      ch = sc.nextLine();
      if (ch.equalsIgnoreCase("Y")) {
        // Once validation completed update the initial deployment with latest image
        patchDeployment(applicationName, newImagePath);
        System.out.println(applicationName + " is updated with " + newImagePath);
        deleteResources(applicationName + "-canary");
        System.out.println("Canary Ingress and Deployment deleted");
        break;
      }
    } while (!ch.equalsIgnoreCase("Y"));
  }
}
