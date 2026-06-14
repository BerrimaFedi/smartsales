export enum Role {
  ADMIN      = 'ADMIN',
  MANAGER    = 'MANAGER',
  COMMERCIAL = 'COMMERCIAL',
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  role: Role;
}

export interface AuthResponse {
  token: string;
  username: string;
  role: Role;
}
