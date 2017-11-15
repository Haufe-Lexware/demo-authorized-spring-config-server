Authorized Spring Config Server Demo
====================================

This project demonstrates the use of a 
[Spring Cloud Config](https://cloud.spring.io/spring-cloud-config/) server
that requires HTTP basic authentication from its client. Both client and server learn 
the relevant credentials from a lookup in a 
[Hashicorp Vault](https://www.vaultproject.io/) instance using a 
[Spring Cloud Vault](http://cloud.spring.io/spring-cloud-vault/) client.

The configuration of the config clients must take place during the respective
application's bootstrapping and therefore has no implicit access to the Property sources 
injected by Spring Cloud Vault into the application environment. This demo's workaround 
is based on Spring Cloud Config's 
[service discovery support](http://cloud.spring.io/spring-cloud-static/spring-cloud-config/1.3.3.RELEASE/multi/multi__spring_cloud_config_client.html#discovery-first-bootstrap).

The demo was prepared for a [presentation](https://www.slideshare.net/HaufeDev/externalized-spring-boot-app-configuration)
at a meeting of the Java user group Freiburg in October 2017. 
