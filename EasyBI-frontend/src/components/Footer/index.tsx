import { GithubOutlined } from '@ant-design/icons';
import { DefaultFooter } from '@ant-design/pro-components';
import React from 'react';

const Footer: React.FC = () => {
  return (
    <DefaultFooter
      style={{
        background: 'none',
      }}
      links={[
        // {
        //   key: 'EasyBI',
        //   title: 'EasyBI',
        //   href: 'https://pro.ant.design',
        //   blankTarget: true,
        // },
        {
          key: '',
          title: <GithubOutlined />,
          href: 'https://github.com/WJW386/',
          blankTarget: true,
        },
        // {
        //   key: 'Willow',
        //   title: 'Willow',
        //   href: 'https://ant.design',
        //   blankTarget: true,
        // },
      ]}
    />
  );
};

export default Footer;
