import * as React from 'react'
import { Link } from 'react-router';
import { mount, shallow } from 'enzyme';
import { expect } from 'chai'
import { FormattedMessage } from 'react-intl'
// Import unconnected version of ProjectsComponent. by using bracets {} around component.
// To get the react-redux connect component use "import ProjectsComponent" instead of "import { ProjectsComponent }"
import { ProjectsContainer } from '../../../scripts/portalApp/modules/projects/containers/ProjectsContainer';
import ProjectComponent from '../../../scripts/portalApp/modules/projects/components/ProjectComponent';
import { ProjectsStore } from '../../../scripts/portalApp/modules/projects/types/ProjectTypes';

// Test a component rendering

describe('[PORTAL APP] Testing projects components', () => {
  it('Should render correctly the loading projects message', () => {
    const dispatch = () => { };
    const onLoad = () => { };
    const projectsStyles = {
      link: 'link',
      projectlink: 'projectlink'
    };
    const projects:ProjectsStore = {
      isFetching: true,
      items: [],
      lastUpdate: ''
    }
    let props = {
      projects,
      styles: projectsStyles,
      dispatch :dispatch,
      onLoad: onLoad
    };
    const wrapper = shallow(<ProjectsContainer {...props} />);
    expect(wrapper.equals(<div>Loading projects ... </div>)).to.equal(true);
  });

  it('Should render correctly the projects list', () => {
    const dispatch = () => { };
    const onLoad = () => { };
    const projectsStyles = {
      link: 'link',
      projectlink: 'projectlink'
    };
    const projects:ProjectsStore = {
      isFetching: false,
      items: [{name: 'cdpp'},{name: 'ssalto'}],
      lastUpdate:''
    }

    let props = {
      projects,
      styles: projectsStyles,
      dispatch :dispatch,
      onLoad : onLoad
    };

    const result = (
      <div>
        <p><FormattedMessage id="portalapp.projects.list.title" /></p>
        <ul>
            <ProjectComponent key="cdpp" project={{name: 'cdpp'}} styles={projectsStyles}/>
            <ProjectComponent key= "ssalto" project={{name: 'ssalto'}} styles={projectsStyles}/>
        </ul>
      </div>
    );
    const wrapper = shallow(<ProjectsContainer {...props}/>);
    expect(wrapper.contains(result)).to.equal(true);
  });

  it('Should render correctly a project link', () => {
    const projectsStyles = {
      link: 'link',
      "project-link": 'project-link'
    };
    let props = {
      styles: projectsStyles,
      project: {name: 'cdpp'}
    };

    const result = (
      <li className="link">
        <p>cdpp</p>
          <Link to="/user/cdpp" className="project-link">
            <FormattedMessage id="project.user.access.link" />
          </Link>
          <Link to="/admin/cdpp" className="project-link">
            <FormattedMessage id="project.admin.access.link" />
          </Link>
      </li>
    )
    const wrapper = shallow(<ProjectComponent {...props}/>);
    // To log result
    //console.log(wrapper.debug());
    expect(wrapper.contains(result)).to.equal(true);
  });

});
