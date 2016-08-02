import { assert } from "chai";
import Schemas from "../../../../scripts/common/api/schemas/index";
import { normalize } from "normalizr";

describe ('[COMMON] Testing schemas', () => {

  it ('should exist', () => {
    assert.isNotNull (Schemas.PROJECT_ARRAY);
    assert.isNotNull (Schemas.PROJECT);
    assert.isNotNull (Schemas.PROJECT_ADMIN);
    assert.isNotNull (Schemas.PROJECT_ADMIN_ARRAY);
    assert.isNotNull (Schemas.USER);
    assert.isNotNull (Schemas.USER_ARRAY);
  })

  it ('should handle project users', () => {
    let response: any = [
      {
        "email": "john.constantine@...",
        "firstName": "John",
        "lastName": "Constantine",
        "login": "jconstantine",
        "password": "passw0rd",
        "status": 0,
        "links": [
          {
            "rel": "self",
            "href": "http://localhost:8080/api/project-admin?name=John"
          },
          {
            "rel": "role",
            "href": "http://localhost:8080/api/project-admin?name=John"
          },
          {
            "rel": "projet",
            "href": "http://localhost:8080/api/project?name=John"
          },
          {
            "rel": "projetUser",
            "href": "http://localhost:8080/api/project?name=John"
          }
        ]
      },
      {
        "email": "fbar@...",
        "firstName": "Foo",
        "lastName": "Bar",
        "login": "fbar",
        "password": "passw0rd",
        "status": 0,
        "links": [
          {
            "rel": "self",
            "href": "http://localhost:8080/api/project-admin?name=Foo"
          },
          {
            "rel": "role",
            "href": "http://localhost:8080/api/project-admin?name=Foo"
          },
          {
            "rel": "projet",
            "href": "http://localhost:8080/api/project?name=Foo"
          },
          {
            "rel": "projetUser",
            "href": "http://localhost:8080/api/project?name=Foo"
          }
        ]
      }
    ]
    const expectedResult: any = {"entities":{"user":{"http://localhost:8080/api/project-admin?name=John":{"email":"john.constantine@...","firstName":"John","lastName":"Constantine","login":"jconstantine","password":"passw0rd","status":0,"links":[{"rel":"self","href":"http://localhost:8080/api/project-admin?name=John"},{"rel":"role","href":"http://localhost:8080/api/project-admin?name=John"},{"rel":"projet","href":"http://localhost:8080/api/project?name=John"},{"rel":"projetUser","href":"http://localhost:8080/api/project?name=John"}]},"http://localhost:8080/api/project-admin?name=Foo":{"email":"fbar@...","firstName":"Foo","lastName":"Bar","login":"fbar","password":"passw0rd","status":0,"links":[{"rel":"self","href":"http://localhost:8080/api/project-admin?name=Foo"},{"rel":"role","href":"http://localhost:8080/api/project-admin?name=Foo"},{"rel":"projet","href":"http://localhost:8080/api/project?name=Foo"},{"rel":"projetUser","href":"http://localhost:8080/api/project?name=Foo"}]}}},"result":["http://localhost:8080/api/project-admin?name=John","http://localhost:8080/api/project-admin?name=Foo"]}
    const result = normalize (response, Schemas.USER_ARRAY);
    assert.deepEqual (result, expectedResult)
  })
})
