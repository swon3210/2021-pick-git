import styled, { css } from "styled-components";

export const Container = styled.div`
  width: 20.5rem;
  height: 39rem;
  padding: 1rem 0;
  overflow-y: auto;
`;

export const Grid = styled.div`
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  row-gap: 2px;
  column-gap: 2px;
`;

export const GridHeading = styled.h1``;

export const GridItem = styled.div<{ imageUrl: string }>(
  ({ imageUrl }) => css`
    width: 100%;
    padding-top: 100%;
    position: relative;
    cursor: pointer;

    :hover {
      filter: brightness(1.1);
    }

    :active {
      filter: brightness(0.9);
    }

    ::after {
      content: "";
      position: absolute;
      top: 0;
      right: 0;
      bottom: 0;
      left: 0;
      background: url(${imageUrl}) center/cover;
    }
  `
);
