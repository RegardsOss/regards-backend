import React, {Component, PropTypes } from 'react'
import {reduxForm} from 'redux-form';
import icons from 'stylesheets/foundation-icons/foundation-icons.scss'

class ProjectConfigurationComponent extends Component {
  render() {
    const {
      show,
      onSaveClick,
      onCancelClick,
      onSubmit,
      fields: {projectName},
      resetForm,
      handleSubmit,
      submitting,
      styles
    } = this.props;

    if(show)
      return (
        <div className={styles.row} >
          <form onSubmit={handleSubmit}>
            <div>
              <input type="text" placeholder="Project Name" {...projectName}/>
            </div>
            <div>
              <button disabled={submitting}>
                {submitting ? <i/> : <i/>} Save
              </button>
              <button type="button" disabled={submitting} onClick={onCancelClick}>
                Cancel
              </button>
            </div>
          </form>
        </div>
      );
    else
      return null
  }
}

ProjectConfigurationComponent.propTypes = {
  fields: PropTypes.object.isRequired,
  handleSubmit: PropTypes.func.isRequired,
  resetForm: PropTypes.func.isRequired,
  submitting: PropTypes.bool.isRequired
}

// const validate = values => {
//   const errors = {}
//   if (!values.username) {
//     errors.username = 'Required'
//   } else if (values.username.length > 15) {
//     errors.username = 'Must be 15 characters or less'
//   }
//   return errors
// }

export default reduxForm({ // <----- THIS IS THE IMPORTANT PART!
  form: 'ProjectConfigurationForm',                           // a unique name for this form
  fields: ['projectName'] // all the fields in your form
  // validate
})(ProjectConfigurationComponent);
