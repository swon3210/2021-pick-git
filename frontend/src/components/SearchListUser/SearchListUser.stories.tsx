import { Story } from "@storybook/react";
import LoggedInWrapper from "../../../.storybook/utils/LoggedInWrapper";

import SearchListUser, { Props } from "./SearchListUser";

export default {
  title: "Components/SearchListUser",
  component: SearchListUser,
};

const Template: Story<Props> = (args) => (
  <LoggedInWrapper>
    <SearchListUser {...args} />
  </LoggedInWrapper>
);

export const Default = Template.bind({});
Default.args = {
  isFetchingNextPage: false,
  onIntersect: () => {},
  users: [
    {
      imageUrl:
        "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=1050&q=80",
      username: "chris",
      following: true,
    },
    {
      imageUrl:
        "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=1050&q=80",
      username: "beuccol",
      following: false,
    },
  ],
};
