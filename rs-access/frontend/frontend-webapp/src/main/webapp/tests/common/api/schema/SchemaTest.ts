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
    const  response: any = [{"email":"john.constantine@...","firstName":"John","lastName":"Constantine","login":"jconstantine","password":"passw0rd","status":0,"projectUsers":[{"status":0,"lastConnection":1470743365500,"lastUpdate":1470743365500,"role":{"nom":"Guest","parentRole":null,"permissions":[],"links":[{"rel":"self","href":"http://localhost:8080/api/project-admin?name=azertui"}]},"links":[{"rel":"self","href":"http://localhost:8080/api/project-admin?name=azertui"}]}],"links":[{"rel":"self","href":"http://localhost:8080/api/project-admin?name=John"},{"rel":"role","href":"http://localhost:8080/api/project-admin?name=John"},{"rel":"projet","href":"http://localhost:8080/api/project?name=John"},{"rel":"projetUser","href":"http://localhost:8080/api/project?name=John"}]},{"email":"fbar@...","firstName":"Foo","lastName":"Bar","login":"fbar","password":"passw0rd","status":0,"projectUsers":[],"links":[{"rel":"self","href":"http://localhost:8080/api/project-admin?name=Foo"},{"rel":"role","href":"http://localhost:8080/api/project-admin?name=Foo"},{"rel":"projet","href":"http://localhost:8080/api/project?name=Foo"},{"rel":"projetUser","href":"http://localhost:8080/api/project?name=Foo"}]}]

    const expectedResult: any = {"entities":{"users":{"http://localhost:8080/api/project-admin?name=John":{"email":"john.constantine@...","firstName":"John","lastName":"Constantine","login":"jconstantine","password":"passw0rd","status":0,"projectUsers":["http://localhost:8080/api/project-admin?name=azertui"],"links":[{"rel":"self","href":"http://localhost:8080/api/project-admin?name=John"},{"rel":"role","href":"http://localhost:8080/api/project-admin?name=John"},{"rel":"projet","href":"http://localhost:8080/api/project?name=John"},{"rel":"projetUser","href":"http://localhost:8080/api/project?name=John"}]},"http://localhost:8080/api/project-admin?name=Foo":{"email":"fbar@...","firstName":"Foo","lastName":"Bar","login":"fbar","password":"passw0rd","status":0,"projectUsers":[],"links":[{"rel":"self","href":"http://localhost:8080/api/project-admin?name=Foo"},{"rel":"role","href":"http://localhost:8080/api/project-admin?name=Foo"},{"rel":"projet","href":"http://localhost:8080/api/project?name=Foo"},{"rel":"projetUser","href":"http://localhost:8080/api/project?name=Foo"}]}},"projectUsers":{"http://localhost:8080/api/project-admin?name=azertui":{"status":0,"lastConnection":1470743365500,"lastUpdate":1470743365500,"role":"http://localhost:8080/api/project-admin?name=azertui","links":[{"rel":"self","href":"http://localhost:8080/api/project-admin?name=azertui"}]}},"roles":{"http://localhost:8080/api/project-admin?name=azertui":{"nom":"Guest","parentRole":null,"permissions":[],"links":[{"rel":"self","href":"http://localhost:8080/api/project-admin?name=azertui"}]}}},"result":["http://localhost:8080/api/project-admin?name=John","http://localhost:8080/api/project-admin?name=Foo"]}

    const result = normalize (response, Schemas.USER_ARRAY);
    console.log(JSON.stringify(result))
    assert.deepEqual (result, expectedResult)
  })
})
