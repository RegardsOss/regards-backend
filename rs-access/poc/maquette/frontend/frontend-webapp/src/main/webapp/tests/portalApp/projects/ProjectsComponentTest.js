import React from 'react'
import { Link } from 'react-router';
import { mount, shallow } from 'enzyme';
import { expect } from 'chai'
// Import unconnected version of ProjectsComponent. by using bracets {} around component.
// To get the react-redux connect component use "import ProjectsComponent" instead of "import { ProjectsComponent }"
import { ProjectsComponent } from '../../../scripts/portalApp/modules/projects/components/ProjectsComponent';
import ProjectComponent from '../../../scripts/portalApp/modules/projects/components/ProjectComponent';

// Test a component rendering

describe('Testing projects components', () => {
  it('Should render correctly the loading projects message', () => {
    const dispatch = () => { };
    const projectsStyles = {
      link: 'link',
      projectlink: 'projectlink'
    };
    let props = {
      projects: {
        isFetching: true
      },
      styles: projectsStyles,
      dispatch :dispatch
    };
    const wrapper = shallow(<ProjectsComponent {...props}/>);
    expect(wrapper.equals(<div>Loading projects ... </div>)).to.equal(true);
  });

  it('Should render correctly the projects list', () => {
    const dispatch = () => { };
    const projectsStyles = {
      link: 'link',
      projectlink: 'projectlink'
    };
    let props = {
      projects: {
        isFetching: false,
        items: [{name: 'cdpp'},{name: 'ssalto'}]
      },
      styles: projectsStyles,
      dispatch :dispatch
    };

    const result = (
      <div>
        <p>Available projects on REGARDS instance :</p>
        <ul>
            <ProjectComponent key="cdpp" project={{name: 'cdpp'}} styles={projectsStyles}/>
            <ProjectComponent key= "ssalto" project={{name: 'ssalto'}} styles={projectsStyles}/>
        </ul>
      </div>
    );
    const wrapper = shallow(<ProjectsComponent {...props}/>);
    expect(wrapper.contains(result)).to.equal(true);
  });

  it('Should render correctly a project link', () => {
    const projectsStyles = {
      link: 'link',
      projectlink: 'projectlink'
    };
    let props = {
      styles: projectsStyles,
      project: {name: 'cdpp'}
    };

    const result = (
      <li className="link">
        <p>cdpp</p>
          <Link to="/user/cdpp" className="projectlink">ihm user</Link>
          <Link to="/admin/cdpp" className="projectlink">ihm admin</Link>
      </li>
    )
    const wrapper = shallow(<ProjectComponent {...props}/>);
    expect(wrapper.contains(result)).to.equal(true);
  });

});
