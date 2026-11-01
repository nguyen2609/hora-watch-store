export const PASSWORD_HINT = '8 to 15 characters.';

export function isValidPassword(value) {
  // Count Unicode code points, matching Java's PasswordHasher validation.
  const length = [...value].length;
  return Boolean(value.trim()) && length >= 8 && length <= 15;
}
