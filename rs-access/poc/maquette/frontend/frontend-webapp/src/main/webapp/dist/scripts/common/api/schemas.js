"use strict";
const normalizr_1 = require('normalizr');
// We use this Normalizr schemas to transform API responses from a nested form
// to a flat form where repos and users are placed in `entities`, and nested
// JSON objects are replaced with their IDs. This is very convenient for
// consumption by reducers, because we can easily build a normalized tree
// and keep it updated as we fetch more data.
// Read more about Normalizr: https://github.com/paularmstrong/normalizr
const projectSchema = new normalizr_1.Schema('projects', {
    idAttribute: project => project.links[0].href
});
const projectAdminSchema = new normalizr_1.Schema('projectAdmins', {
    idAttribute: projectAdmin => projectAdmin.links[0].href
});
projectSchema.define({
    admins: normalizr_1.arrayOf(projectAdminSchema)
});
projectAdminSchema.define({
    projects: normalizr_1.arrayOf(projectSchema)
});
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = {
    PROJECT: projectSchema,
    PROJECT_ARRAY: normalizr_1.arrayOf(projectSchema),
    PROJECT_ADMIN: projectAdminSchema,
    PROJECT_ADMIN_ARRAY: normalizr_1.arrayOf(projectAdminSchema)
};
//# sourceMappingURL=schemas.js.map