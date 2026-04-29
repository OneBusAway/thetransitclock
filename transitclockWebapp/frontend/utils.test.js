import { describe, it, expect } from 'vitest';
import { classNames } from './utils.js';

describe('classNames', () => {
  it('joins truthy parts with spaces', () => {
    expect(classNames('a', 'b', 'c')).toBe('a b c');
  });

  it('skips falsy parts', () => {
    expect(classNames('a', null, undefined, false, '', 'b')).toBe('a b');
  });
});
