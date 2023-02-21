ktor:
  application:
    modules:
      - com.fabien.ApplicationKt.module
  deployment:
    port: 3001
  development: true
jwt:
  domain:
  audience:
insee:
  baseApi: "api.insee.fr"
  siretApi: "entreprises/sirene/V3/siret"
  authenticationApi: "token"
  base64ConsumerKeySecret: