import rootReducer from "../src/rootReducer"
import { assert } from "chai"
import React from "react"


describe('[MAIN APP] Testing rootReducer', () => {

  it('should exist', () => {
    assert.isNotNull(rootReducer)
  })

})
