package Demo

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import Demo.Data._

class LoginTest extends Simulation {

  // 1. Configuración base
  val httpConf = http.baseUrl(url)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  // 2. Escenario: Login exitoso y uso de token
  val loginSuccessScn = scenario("Login Exitoso y Crear Contacto")
    .exec(http("Login Exitoso")
      .post("users/login")
      .body(StringBody(s"""{"email": "$email", "password": "$password"}""")).asJson
      .check(status.is(200))
      .check(jsonPath("$.token").saveAs("authToken"))
      .check(jsonPath("$.user.email").is(email))
      .check(jsonPath("$.user._id").exists)
    )
    .exec(http("Acceder a /contacts con token")
      .get("contacts")
      .header("Authorization", "Bearer ${authToken}")
      .check(status.in(200, 204)) // puede ser 204 si no hay contactos aún
    )
    .exec(http("Crear Contacto")
      .post("contacts")
      .header("Authorization", "Bearer ${authToken}")
      .body(StringBody(
        """
          {
            "firstName": "Ok",
            "lastName": "Gomez",
            "birthdate": "1970-01-01",
            "email": "jdoe@fake.com",
            "phone": "8005555555",
            "street1": "1 Main St.",
            "street2": "Apartment A",
            "city": "Anytown",
            "stateProvince": "KS",
            "postalCode": "12345",
            "country": "USA"
          }
        """
      )).asJson
      .check(status.is(201))
    )

  // 3. Escenario: Login fallido
  val loginFailScn = scenario("Login Fallido con Credenciales Inválidas")
    .exec(http("Login Fallido")
      .post("users/login")
      .body(StringBody("""{"email": "wrong@email.com", "password": "wrongpass"}""")).asJson
      .check(status.is(401))
      .check(jsonPath("$.message").is("Incorrect email or password"))
    )

  // 4. Carga de usuarios
  setUp(
    loginSuccessScn.inject(rampUsers(10).during(50)),
    loginFailScn.inject(atOnceUsers(1))
  ).protocols(httpConf)
}

