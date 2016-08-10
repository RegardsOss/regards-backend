import { Schema, arrayOf } from "normalizr"
import { find } from "lodash"
import { HATEOASLink, MISSING_LINK } from "./common"

interface ResultingProject {
  links: Array<HATEOASLink>
}

// Read more about Normalizr: https://github.com/paularmstrong/normalizr
const projectSchema = new Schema('projects', {
  idAttribute: (project: ResultingProject) => {
    const itself: HATEOASLink = find(project.links, {"rel": "self"})
    return itself ? itself.href : MISSING_LINK
  }
})


// Schemas for API responses.
export default {
  PROJECT: projectSchema,
  PROJECT_ARRAY: arrayOf(projectSchema)
}
