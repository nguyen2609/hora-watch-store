export const PASSWORD_HINT = 'Từ 8 đến 15 ký tự.';

export function isValidPassword(value) {
  // Count Unicode code points, matching Java's PasswordHasher validation.
  const length = [...value].length;
  return Boolean(value.trim()) && length >= 8 && length <= 15;
}
