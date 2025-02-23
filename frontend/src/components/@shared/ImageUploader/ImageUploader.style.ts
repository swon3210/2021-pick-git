import styled from "styled-components";

export const Container = styled.div`
  display: flex;
  justify-content: center;
  width: 100%;
`;

export const Image = styled.img`
  width: 100%;
  max-width: 420px;
  transition: opacity 0.5s, box-shadow 0.5s;
  cursor: pointer;

  :hover {
    opacity: 0.85;
    box-shadow: 1px 2px 6px rgba(0, 0, 0, 0.2);
  }
`;
