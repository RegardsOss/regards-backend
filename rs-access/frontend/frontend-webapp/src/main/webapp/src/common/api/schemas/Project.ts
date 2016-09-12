import { Schema, arrayOf } from "normalizr"
import { Project } from "../../models/projects/types"

// Read more about Normalizr: https://github.com/paularmstrong/normalizr
const projectSchema = new Schema('projects', {
  idAttribute: (project: Project) => {
    return project.projectId
  }
})

// Schemas for API responses.
export default {
  PROJECT: projectSchema,
  PROJECT_ARRAY: arrayOf(projectSchema)
}
